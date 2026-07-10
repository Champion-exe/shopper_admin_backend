package com.shopper.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private Long id;

    private String orderNo;
    private Long userId;
    private String status;
    private Double totalAmount;
    private Double discountAmount;
    private Double payAmount;
    private String paymentMethod;
    private Long addressId;
    private String consignee;
    private String phone;
    private String address;
    private String remark;

    @Column(length = 4000)
    private String items;

    private String createTime;
    private String paymentTime;
    private String deliveryTime;
    private String confirmTime;

    /** JSON 数组 → 字符串 存入 DB */
    public void setItems(Object value) {
        if (value == null) {
            this.items = null;
        } else if (value instanceof String s) {
            this.items = s;
        } else {
            try {
                this.items = new ObjectMapper().writeValueAsString(value);
            } catch (JsonProcessingException e) {
                this.items = "[]";
            }
        }
    }

    public String getItems() {
        return items;
    }
}
