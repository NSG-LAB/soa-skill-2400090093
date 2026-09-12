package com.soa.order.model;

import java.util.List;

public class Order {

    private int userId;
    private int restaurantId;
    private List<String> items;

    public Order() {}

    public int getUserId() {
        return userId;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public List<String> getItems() {
        return items;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }
}
