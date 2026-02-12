package framework.models;


public class Product {
    private final String title;
    private final Double price; // null = no tiene precio

    public Product(String title, Double price) {
        this.title = title;
        this.price = price;
    }

    public String getTitle() { return title; }
    public Double getPrice() { return price; }

    @Override
    public String toString() {
        return title + " | " + (price == null ? "no tiene precio" : "$" + price);
    }
}



