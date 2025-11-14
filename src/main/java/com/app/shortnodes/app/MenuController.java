package com.app.shortnodes.app;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/apps/{appId}")
public class MenuController {
    private final MenuRepository menuRepo;
    private final MenuOptionRepository optionRepo;
    private final ConditionRepository conditionRepo;
    private final AppEvents events;

    public MenuController(MenuRepository menuRepo, MenuOptionRepository optionRepo, ConditionRepository conditionRepo, AppEvents events) {
        this.menuRepo = menuRepo;
        this.optionRepo = optionRepo;
        this.conditionRepo = conditionRepo;
        this.events = events;
    }

    @GetMapping("/menus")
    public List<Menu> listMenus(@PathVariable UUID appId) {
        return menuRepo.findByAppId(appId);
    }

    @GetMapping("/menus/entries")
    public List<Menu> entryMenus(@PathVariable UUID appId) {
        return menuRepo.findByAppId(appId).stream().filter(Menu::isEntry).collect(Collectors.toList());
    }

    public static class MenuTreeItem {
        public Menu menu;
        public List<MenuOption> options;
        public Map<UUID, Condition> conditionsByOption = new HashMap<>();
        
        // Add position fields to the response
        public Double positionX;
        public Double positionY;
    }

    @GetMapping("/menus/tree")
    public List<MenuTreeItem> menuTree(@PathVariable UUID appId) {
        List<Menu> menus = menuRepo.findByAppId(appId);
        List<MenuTreeItem> out = new ArrayList<>();
        for (Menu m : menus) {
            MenuTreeItem item = new MenuTreeItem();
            item.menu = m;
            item.positionX = m.getPositionX();
            item.positionY = m.getPositionY();
            List<MenuOption> opts = optionRepo.findByMenuIdOrderByKeyIndexAsc(m.getId());
            item.options = opts;
            for (MenuOption o : opts) {
                conditionRepo.findByOptionId(o.getId()).ifPresent(c -> item.conditionsByOption.put(o.getId(), c));
            }
            out.add(item);
        }
        return out;
    }

    @GetMapping("/subflows")
    public Map<String, List<Menu>> subflows(@PathVariable UUID appId) {
        List<Menu> menus = menuRepo.findByAppId(appId);
        Map<String, List<Menu>> groups = new HashMap<>();
        for (Menu m : menus) {
            String group = m.getSubflowId();
            if (group == null || group.isBlank()) continue;
            groups.computeIfAbsent(group, k -> new ArrayList<>()).add(m);
        }
        return groups;
    }

    // Simple upserts
    @PostMapping("/menus")
    public ResponseEntity<Menu> upsertMenu(@PathVariable UUID appId, @RequestBody Menu menu) {
        if (menu.getId() == null) menu.setId(UUID.randomUUID());
        menu.setAppId(appId);
        Menu saved = menuRepo.save(menu);
        events.publishChange(appId, "menu");
        return ResponseEntity.created(URI.create("/api/apps/" + appId + "/menus/" + saved.getId())).body(saved);
    }

    @PostMapping("/menus/{menuId}/options")
    public ResponseEntity<MenuOption> upsertOption(@PathVariable UUID appId, @PathVariable UUID menuId, @RequestBody MenuOption option) {
        if (option.getId() == null) option.setId(UUID.randomUUID());
        option.setMenuId(menuId);
        MenuOption saved = optionRepo.save(option);
        events.publishChange(appId, "option");
        return ResponseEntity.created(URI.create("/api/apps/" + appId + "/menus/" + menuId + "/options/" + saved.getId())).body(saved);
    }

    @PostMapping("/options/{optionId}/condition")
    public ResponseEntity<Condition> upsertCondition(@PathVariable UUID appId, @PathVariable UUID optionId, @RequestBody Condition condition) {
        if (condition.getId() == null) condition.setId(UUID.randomUUID());
        condition.setOptionId(optionId);
        Condition saved = conditionRepo.save(condition);
        events.publishChange(appId, "condition");
        return ResponseEntity.created(URI.create("/api/apps/" + appId + "/options/" + optionId + "/condition")).body(saved);
    }

    // Bulk publish format matching editor graph
    public static class PublishRequest {
        public UUID appId; // must match path
        public List<MenuPayload> menus;
    }

    public static class MenuPayload {
        public UUID id;
        public String name;
        public String text;
        public String mode;
        public boolean entry;
        public String subflowId;
        public List<OptionPayload> options;
        
        // Add position fields
        public Double positionX;
        public Double positionY;
    }

    public static class OptionPayload {
        public UUID id;
        public int keyIndex;
        public String label;
        public UUID targetMenuId;
        public ConditionPayload condition; // optional
    }

    public static class ConditionPayload {
        public UUID id;
        public String code;
        public UUID invalidTargetMenuId;
    }

    @PostMapping("/publish")
    @Transactional
    public ResponseEntity<Map<String, Object>> publish(@PathVariable UUID appId, @RequestBody PublishRequest body) {
        // Upsert menus (first pass) and keep deterministic mapping by index
        List<UUID> savedMenuIds = new ArrayList<>();
        for (int i = 0; i < body.menus.size(); i++) {
            MenuPayload mp = body.menus.get(i);
            UUID id = mp.id != null ? mp.id : UUID.randomUUID();
            Menu m = new Menu();
            m.setId(id);
            m.setAppId(appId);
            m.setName(mp.name != null ? mp.name : "Menu");
            m.setText(mp.text != null ? mp.text : "");
            m.setMode(mp.mode != null ? mp.mode : "menu");
            m.setEntry(mp.entry);
            m.setSubflowId(mp.subflowId);
            // Add position fields
            m.setPositionX(mp.positionX);
            m.setPositionY(mp.positionY);
            menuRepo.save(m);
            // ensure list index aligns with second pass
            if (savedMenuIds.size() == i) savedMenuIds.add(id); else savedMenuIds.set(i, id);
            // clear old options for deterministic publish
            optionRepo.findByMenuIdOrderByKeyIndexAsc(id).forEach(o -> conditionRepo.findByOptionId(o.getId()).ifPresent(c -> conditionRepo.deleteById(c.getId())));
            optionRepo.findByMenuIdOrderByKeyIndexAsc(id).forEach(o -> optionRepo.deleteById(o.getId()));
        }

        // Upsert options and conditions (second pass using index mapping)
        for (int i = 0; i < body.menus.size(); i++) {
            MenuPayload mp = body.menus.get(i);
            UUID menuId = savedMenuIds.get(i);
            if (menuId == null) continue;
            if (mp.options == null) continue;
            for (OptionPayload op : mp.options) {
                MenuOption o = new MenuOption();
                o.setId(op.id != null ? op.id : UUID.randomUUID());
                o.setMenuId(menuId);
                o.setKeyIndex(op.keyIndex);
                o.setLabel(op.label != null ? op.label : "");
                if (op.targetMenuId != null) o.setTargetMenuId(op.targetMenuId);
                optionRepo.save(o);
                if (op.condition != null) {
                    ConditionPayload cp = op.condition;
                    Condition c = new Condition();
                    c.setId(cp.id != null ? cp.id : UUID.randomUUID());
                    c.setOptionId(o.getId());
                    c.setCode(cp.code);
                    c.setInvalidTargetMenuId(cp.invalidTargetMenuId);
                    conditionRepo.save(c);
                }
            }
        }

        // Remove menus that are no longer present (and cascade delete their options/conditions)
        Set<UUID> keepIds = new HashSet<>(savedMenuIds);
        for (Menu existing : menuRepo.findByAppId(appId)) {
            if (!keepIds.contains(existing.getId())) {
                List<MenuOption> toRemove = optionRepo.findByMenuIdOrderByKeyIndexAsc(existing.getId());
                for (MenuOption o : toRemove) {
                    conditionRepo.findByOptionId(o.getId()).ifPresent(c -> conditionRepo.deleteById(c.getId()));
                    optionRepo.deleteById(o.getId());
                }
                menuRepo.deleteById(existing.getId());
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        // Push live snapshot over websocket
        List<MenuTreeItem> snapshot = menuTree(appId);
        events.publishTree(appId, snapshot);
        return ResponseEntity.ok(resp);
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable UUID appId) {
        return events.subscribe(appId);
    }
}


