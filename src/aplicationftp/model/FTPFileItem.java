/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package aplicationftp.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FTPFileItem {

    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty size = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty date = new SimpleStringProperty();

    public FTPFileItem(String name, String size, String type, String date) {
        this.name.set(name);
        this.size.set(size);
        this.type.set(type);
        this.date.set(date);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        name.set(value);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getSize() {
        return size.get();
    }

    public void setSize(String value) {
        size.set(value);
    }

    public StringProperty sizeProperty() {
        return size;
    }

    public String getType() {
        return type.get();
    }

    public void setType(String value) {
        type.set(value);
    }

    public StringProperty typeProperty() {
        return type;
    }

    public String getDate() {
        return date.get();
    }

    public void setDate(String value) {
        date.set(value);
    }

    public StringProperty dateProperty() {
        return date;
    }
}
