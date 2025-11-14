package com.app.shortnodes.app;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.*;

@Controller
public class GraphWsController {
    private final MenuRepository menuRepo;
    private final MenuOptionRepository optionRepo;
    private final ConditionRepository conditionRepo;
    private final AppEvents events;

    public GraphWsController(MenuRepository menuRepo, MenuOptionRepository optionRepo, ConditionRepository conditionRepo, AppEvents events) {
        this.menuRepo = menuRepo;
        this.optionRepo = optionRepo;
        this.conditionRepo = conditionRepo;
        this.events = events;
    }

    @MessageMapping("/apps/{appId}/request-tree")
    public void requestTree(@DestinationVariable UUID appId) {
        List<MenuController.MenuTreeItem> snapshot = new ArrayList<>();
        for (Menu m : menuRepo.findByAppId(appId)) {
            MenuController.MenuTreeItem item = new MenuController.MenuTreeItem();
            item.menu = m;
            item.positionX = m.getPositionX();
            item.positionY = m.getPositionY();
            List<MenuOption> opts = optionRepo.findByMenuIdOrderByKeyIndexAsc(m.getId());
            item.options = opts;
            for (MenuOption o : opts) {
                conditionRepo.findByOptionId(o.getId()).ifPresent(c -> item.conditionsByOption.put(o.getId(), c));
            }
            snapshot.add(item);
        }
        events.publishTree(appId, snapshot);
    }

    @MessageMapping("/apps/{appId}/save-graph")
    public void saveGraph(@DestinationVariable UUID appId, MenuController.PublishRequest body) {
        // Use the same logic as publish but with a different message type
        if (body == null || body.menus == null) return;
        
        // Upsert menus first, keep index mapping
        List<UUID> savedMenuIds = new ArrayList<>();
        for (int i = 0; i < body.menus.size(); i++) {
            MenuController.MenuPayload mp = body.menus.get(i);
            UUID id = mp.id != null ? mp.id : UUID.randomUUID();
            Menu m = new Menu();
            m.setId(id);
            m.setAppId(appId);
            m.setName(mp.name != null ? mp.name : "Menu");
            m.setText(mp.text != null ? mp.text : "");
            m.setEntry(mp.entry);
            m.setSubflowId(mp.subflowId);
            m.setPositionX(mp.positionX);
            m.setPositionY(mp.positionY);
            menuRepo.save(m);
            if (savedMenuIds.size() == i) savedMenuIds.add(id); else savedMenuIds.set(i, id);
            optionRepo.findByMenuIdOrderByKeyIndexAsc(id).forEach(o -> conditionRepo.findByOptionId(o.getId()).ifPresent(c -> conditionRepo.deleteById(c.getId())));
            optionRepo.findByMenuIdOrderByKeyIndexAsc(id).forEach(o -> optionRepo.deleteById(o.getId()));
        }
        
        for (int i = 0; i < body.menus.size(); i++) {
            MenuController.MenuPayload mp = body.menus.get(i);
            UUID menuId = savedMenuIds.get(i);
            if (menuId == null || mp.options == null) continue;
            for (MenuController.OptionPayload op : mp.options) {
                MenuOption o = new MenuOption();
                o.setId(op.id != null ? op.id : UUID.randomUUID());
                o.setMenuId(menuId);
                o.setKeyIndex(op.keyIndex);
                o.setLabel(op.label != null ? op.label : "");
                if (op.targetMenuId != null) o.setTargetMenuId(op.targetMenuId);
                optionRepo.save(o);
                if (op.condition != null) {
                    MenuController.ConditionPayload cp = op.condition;
                    Condition c = new Condition();
                    c.setId(cp.id != null ? cp.id : UUID.randomUUID());
                    c.setOptionId(o.getId());
                    c.setCode(cp.code);
                    c.setInvalidTargetMenuId(cp.invalidTargetMenuId);
                    conditionRepo.save(c);
                }
            }
        }
        
        // Remove menus not present
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
        
        // Send confirmation back
        events.publishSaveConfirmation(appId, "Graph saved successfully");
        
        // Also send updated tree
        List<MenuController.MenuTreeItem> snapshot = new ArrayList<>();
        for (Menu m : menuRepo.findByAppId(appId)) {
            MenuController.MenuTreeItem item = new MenuController.MenuTreeItem();
            item.menu = m;
            item.positionX = m.getPositionX();
            item.positionY = m.getPositionY();
            List<MenuOption> opts = optionRepo.findByMenuIdOrderByKeyIndexAsc(m.getId());
            item.options = opts;
            for (MenuOption o : opts) {
                conditionRepo.findByOptionId(o.getId()).ifPresent(c -> item.conditionsByOption.put(o.getId(), c));
            }
            snapshot.add(item);
        }
        
        // Log the snapshot to debug mode field
        System.out.println("WebSocket: Sending tree snapshot with " + snapshot.size() + " menus");
        for (MenuController.MenuTreeItem item : snapshot) {
            System.out.println("Menu: " + item.menu.getName() + ", mode: " + item.menu.getMode());
        }
        events.publishTree(appId, snapshot);
    }

    @MessageMapping("/apps/{appId}/publish")
    public void publish(@DestinationVariable UUID appId, MenuController.PublishRequest body) {
        if (body == null || body.menus == null) return;
        // Upsert menus first, keep index mapping
        List<UUID> savedMenuIds = new ArrayList<>();
        for (int i = 0; i < body.menus.size(); i++) {
            MenuController.MenuPayload mp = body.menus.get(i);
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
            if (savedMenuIds.size() == i) savedMenuIds.add(id); else savedMenuIds.set(i, id);
            optionRepo.findByMenuIdOrderByKeyIndexAsc(id).forEach(o -> conditionRepo.findByOptionId(o.getId()).ifPresent(c -> conditionRepo.deleteById(c.getId())));
            optionRepo.findByMenuIdOrderByKeyIndexAsc(id).forEach(o -> optionRepo.deleteById(o.getId()));
        }
        for (int i = 0; i < body.menus.size(); i++) {
            MenuController.MenuPayload mp = body.menus.get(i);
            UUID menuId = savedMenuIds.get(i);
            if (menuId == null || mp.options == null) continue;
            for (MenuController.OptionPayload op : mp.options) {
                MenuOption o = new MenuOption();
                o.setId(op.id != null ? op.id : UUID.randomUUID());
                o.setMenuId(menuId);
                o.setKeyIndex(op.keyIndex);
                o.setLabel(op.label != null ? op.label : "");
                if (op.targetMenuId != null) o.setTargetMenuId(op.targetMenuId);
                optionRepo.save(o);
                if (op.condition != null) {
                    MenuController.ConditionPayload cp = op.condition;
                    Condition c = new Condition();
                    c.setId(cp.id != null ? cp.id : UUID.randomUUID());
                    c.setOptionId(o.getId());
                    c.setCode(cp.code);
                    c.setInvalidTargetMenuId(cp.invalidTargetMenuId);
                    conditionRepo.save(c);
                }
            }
        }
        // Remove menus not present
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
        // Snapshot and broadcast over WS
        List<MenuController.MenuTreeItem> snapshot = new ArrayList<>();
        for (Menu m : menuRepo.findByAppId(appId)) {
            MenuController.MenuTreeItem item = new MenuController.MenuTreeItem();
            item.menu = m;
            item.positionX = m.getPositionX();
            item.positionY = m.getPositionY();
            List<MenuOption> opts = optionRepo.findByMenuIdOrderByKeyIndexAsc(m.getId());
            item.options = opts;
            for (MenuOption o : opts) {
                conditionRepo.findByOptionId(o.getId()).ifPresent(c -> item.conditionsByOption.put(o.getId(), c));
            }
            snapshot.add(item);
        }
        
        // Log the snapshot to debug mode field
        System.out.println("WebSocket: Sending initial tree snapshot with " + snapshot.size() + " menus");
        for (MenuController.MenuTreeItem item : snapshot) {
            System.out.println("Menu: " + item.menu.getName() + ", mode: " + item.menu.getMode());
        }
        events.publishTree(appId, snapshot);
    }
}


