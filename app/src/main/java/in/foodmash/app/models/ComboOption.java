package in.foodmash.app.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;

/**
 * Created by Zeke on Sep 11 2015.
 */
public class ComboOption {

    public static final int BULK_ORDER_CONSTANT = 15;
    private int id;
    private int priority;
    private int minCount = 0;
    private String name;
    private String description;
    private ArrayList<ComboOptionDish> selectedComboOptionDishes;
    private ArrayList<ComboOptionDish> comboOptionDishes = new ArrayList<>();

    public ComboOption() {}
    public ComboOption(ComboOption c) {
        this.id = c.id;
        this.priority = c.priority;
        this.name = c.name;
        this.description = c.description;
        this.minCount = c.minCount;
        this.selectedComboOptionDishes = new ArrayList<>();
        for(ComboOptionDish comboDish: c.getSelectedComboOptionDishes())
            this.selectedComboOptionDishes.add(new ComboOptionDish(comboDish));
        this.comboOptionDishes = new ArrayList<>();
        for(ComboOptionDish comboDish: c.comboOptionDishes)
            this.comboOptionDishes.add(new ComboOptionDish(comboDish));
    }

    public int getId() { return id; }
    public int getPriority() { return priority; }
    public int getMinCount() { return minCount; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    @JsonProperty("combo_option_dishes") public ArrayList<ComboOptionDish> getSelectedComboOptionDishes() { return new ArrayList<>(selectedComboOptionDishes); }

    @JsonIgnore public ArrayList<ComboOptionDish> getComboOptionDishes() { return comboOptionDishes; }
    public void setId(int id) { this.id = id; }
    public void setPriority(int priority) { this.priority = priority; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }

    public void setSelectedComboOptionDishes(ArrayList<ComboOptionDish> selectedComboOptionDishes) { this.selectedComboOptionDishes = selectedComboOptionDishes; }
    protected int getComprisedDishesQuantity() {
        int quantity=0;
        for(ComboOptionDish comboDish: this.selectedComboOptionDishes)
            quantity+=comboDish.getQuantity();
//        try { if(quantity<minCount) throw new Exception("Quantity less than minCount. Quantity: " + quantity + ", MinCount: " + minCount); }
//        catch (Exception e) { e.printStackTrace(); }
        return quantity;
    }
    public LinkedHashSet<Dish.Label> getLabels() {
        LinkedHashSet<Dish.Label> labels = new LinkedHashSet<>();
        int veg = 0;
        int non_veg = 0;
        int egg = 0;
        for (ComboOptionDish comboDish: comboOptionDishes) {
            switch (comboDish.getDish().getLabel()) {
                case EGG: egg++; break;
                case VEG: veg++; break;
                case NON_VEG: non_veg++; break;
            }
        }
        if(veg>0) labels.add(Dish.Label.VEG);
        if(egg>0) labels.add(Dish.Label.EGG);
        if(non_veg>0) labels.add(Dish.Label.NON_VEG);
        return labels;
    }

    public void setMinCount(int minCount) {
        this.minCount = minCount;
        if(comboOptionDishes!=null && comboOptionDishes.size()!=0)
            resetSelectedComboOptionDishes();
    }
    public ComboOptionDish fetch(int id) {
        ComboOptionDish requiredComboDish = null;
        for(ComboOptionDish comboDish: this.comboOptionDishes)
            if(comboDish.getId()==id) requiredComboDish = comboDish;
        return requiredComboDish;
    }
    public boolean incrementQuantity(ComboOptionDish comboOptionDish) {
        if(this.getComprisedDishesQuantity()+1>BULK_ORDER_CONSTANT) return false;
        if(!getSelectedComboOptionDishes().contains(comboOptionDish)) addToSelected(comboOptionDish);
        else comboOptionDish.setQuantity(comboOptionDish.getQuantity()+1);
        return true;
    }
    public boolean decrementQuantity(ComboOptionDish comboDish) {
        if(this.getComprisedDishesQuantity()-1< 0) return false;
        if(!getSelectedComboOptionDishes().contains(comboDish)) return false;
        if(comboDish.getQuantity()==1) return removeFromSelected(comboDish);
        comboDish.setQuantity(comboDish.getQuantity()-1);
        return true;
    }
    public boolean addToSelected(ComboOptionDish comboDish) {
        if(!this.selectedComboOptionDishes.contains(comboDish)) {
            comboDish.incrementQuantity();
            this.selectedComboOptionDishes.add(comboDish);
            return true;
        } else return false;
    }
    public boolean removeFromSelected(ComboOptionDish comboDish) {
        if(this.selectedComboOptionDishes.contains(comboDish))  {
            this.selectedComboOptionDishes.remove(comboDish);
            comboDish.setQuantity(0);
            return true;
        } else return false;
    }
    public void resetSelectedComboOptionDishes() {
        Collections.sort(comboOptionDishes, new Comparator<ComboOptionDish>() {
            @Override
            public int compare(ComboOptionDish lhs, ComboOptionDish rhs) {
                return Float.compare(lhs.getDish().getPrice(), rhs.getDish().getPrice());
            }
        });
        if(selectedComboOptionDishes!=null)
            this.selectedComboOptionDishes.clear();
        if(minCount==0) return;
        comboOptionDishes.get(0).setQuantity(minCount);
        this.selectedComboOptionDishes.add(comboOptionDishes.get(0));
    }
    public void removeAllSelectedComboOptionDishes() {
        for (ComboOptionDish comboOptionDish : this.comboOptionDishes)
            comboOptionDish.setQuantity(0);
        this.selectedComboOptionDishes.clear();
    }
    @JsonProperty public void setComboOptionDishes(ArrayList<ComboOptionDish> comboOptionDishes) {
        this.comboOptionDishes = comboOptionDishes;
        this.selectedComboOptionDishes = new ArrayList<>();
        if(comboOptionDishes != null && comboOptionDishes.size() != 0) {
            Collections.sort(comboOptionDishes, new Comparator<ComboOptionDish>() {
                @Override
                public int compare(ComboOptionDish lhs, ComboOptionDish rhs) {
                    return Float.compare(lhs.getDish().getPrice(), rhs.getDish().getPrice());
                }
            });
        }
        if(minCount>0) this.resetSelectedComboOptionDishes();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ComboOption)) return false;
        if (o == this) return true;
        ComboOption comboOption = (ComboOption) o;
        if(this.id != comboOption.id) return false;
        if(this.selectedComboOptionDishes!=null && comboOption.selectedComboOptionDishes!=null)
            for(ComboOptionDish comboOptionDish: this.selectedComboOptionDishes)
                if(!comboOption.selectedComboOptionDishes.contains(comboOptionDish)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31*hash + this.id;
        for (ComboOptionDish comboDish : this.selectedComboOptionDishes) {
            hash = 31 * hash + comboDish.getId();
            hash = 31 * hash + comboDish.getQuantity();
        }
        return hash;
    }
}
