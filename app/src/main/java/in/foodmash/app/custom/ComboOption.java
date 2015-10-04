package in.foodmash.app.custom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sarav on Sep 11 2015.
 */
public class ComboOption {

    private int id;
    private int priority;
    private String name;
    private String description;
    private ComboDish selectedComboDish;
    private ArrayList<ComboDish> comboOptionDishes = new ArrayList<>();

    public ComboOption() {}
    public ComboOption(ComboOption c) {
        this.id = c.id;
        this.priority = c.priority;
        this.name = c.name;
        this.description = c.description;
        this.selectedComboDish  = c.selectedComboDish;
        this.comboOptionDishes = c.comboOptionDishes;
    }

    public int getId() { return id; }
    public int getPriority() { return priority; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ComboDish getSelectedComboDish() { return selectedComboDish; }
    public ArrayList<ComboDish> getComboOptionDishes() { return new ArrayList<>(comboOptionDishes); }
    public boolean isFromSameRestaurant() {
        Set<Integer> restaurantIdSet = new HashSet<>();
        for (ComboDish comboDish:comboOptionDishes)
            restaurantIdSet.add(comboDish.getDish().getRestaurant().getId());
        return restaurantIdSet.size()==1;
    }
    public ComboDish getSelectedDish() {
        for (ComboDish comboDish:comboOptionDishes)
            if(selectedComboDish.getId()==comboDish.getId())
                return comboDish;
        return null;
    }

    public void setId(int id) { this.id = id; }
    public void setPriority(int priority) { this.priority = priority; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setSelected(ComboDish selectedComboDish) { this.selectedComboDish = selectedComboDish; }
    public void setComboOptionDishes(ArrayList<ComboDish> comboOptionDishes) { this.comboOptionDishes = comboOptionDishes; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ComboOption)) return false;
        if (o == this) return true;
        ComboOption comboOption = (ComboOption) o;
        if (this.comboOptionDishes.size() != comboOption.comboOptionDishes.size()) return false;
        ArrayList<ComboDish> compareComboOptionDishes = comboOption.getComboOptionDishes();
        compareComboOptionDishes.removeAll(this.comboOptionDishes);
        return compareComboOptionDishes.size() == 0 && selectedComboDish == comboOption.selectedComboDish;
    }

}
