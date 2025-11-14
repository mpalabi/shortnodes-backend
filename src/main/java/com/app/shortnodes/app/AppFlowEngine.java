package com.app.shortnodes.app;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AppFlowEngine {
    private final MenuRepository menuRepo;
    private final MenuOptionRepository optionRepo;
    private final ConditionRepository conditionRepo;

    public AppFlowEngine(MenuRepository menuRepo, MenuOptionRepository optionRepo, ConditionRepository conditionRepo) {
        this.menuRepo = menuRepo;
        this.optionRepo = optionRepo;
        this.conditionRepo = conditionRepo;
    }

    public Optional<Menu> entryMenu(UUID appId) {
        List<Menu> menus = menuRepo.findByAppId(appId);
        return menus.stream().filter(Menu::isEntry).findFirst().or(() -> menus.stream().findFirst());
    }

    public String render(Menu menu) {
        StringBuilder sb = new StringBuilder(" ").append(menu.getText());
        List<MenuOption> opts = optionRepo.findByMenuIdOrderByKeyIndexAsc(menu.getId());
        for (MenuOption o : opts) {
            sb.append("\n").append(o.getKeyIndex()).append(". ").append(o.getLabel());
        }
        return sb.toString();
    }

    public Optional<Menu> nextFromInput(Menu current, String input) {
        try {
            int key = Integer.parseInt(input.trim());
            MenuOption opt = optionRepo.findByMenuIdOrderByKeyIndexAsc(current.getId()).stream()
                    .filter(o -> o.getKeyIndex() == key)
                    .findFirst().orElse(null);
            if (opt == null) return Optional.empty();
            // condition check
            Optional<Condition> cond = conditionRepo.findByOptionId(opt.getId());
            if (cond.isPresent()) {
                Condition c = cond.get();
                if (!evaluate(c.getCode(), input)) {
                    if (c.getInvalidTargetMenuId() != null) {
                        return menuRepo.findById(c.getInvalidTargetMenuId());
                    }
                    return Optional.empty();
                }
            }
            if (opt.getTargetMenuId() != null) {
                return menuRepo.findById(opt.getTargetMenuId());
            }
        } catch (NumberFormatException ignored) {}
        return Optional.empty();
    }

    private boolean evaluate(String code, String input) {
        if (code == null || code.isBlank()) return true;
        int first = code.indexOf('/');
        int last = code.lastIndexOf('/');
        if (first != -1 && last > first) {
            String body = code.substring(first + 1, last);
            boolean invert = code.startsWith("!");
            try {
                boolean ok = input.matches(body);
                return invert ? !ok : ok;
            } catch (Exception ignored) { return true; }
        }
        return true;
    }
}


