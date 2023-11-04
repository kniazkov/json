package com.kniazkov.json;

import java.util.*;

/**
 * Array of JSON elements.
 */
public final class JsonArray extends JsonContainer implements List<JsonElement> {
    /**
     * List of elements.
     */
    private final List<JsonElement> list;

    /**
     * Constructor.
     */
    public JsonArray() {
        this(null);
    }

    /**
     * Constructor (for internal use).
     * @param parent The parent of this element
     */
    JsonArray(JsonContainer parent) {
        super(parent);
        list = new ArrayList<>();
    }

    @Override
    JsonElement clone(JsonContainer anotherParent) {
        JsonArray copy = new JsonArray(anotherParent);
        for (JsonElement elem : list) {
            copy.addElement(elem);
        }
        return copy;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append('[');
        boolean flag = false;
        for (JsonElement element : list) {
            if (flag) {
                builder.append(", ");
            }
            flag = true;
            builder.append(element.toString());
        }
        builder.append(']');
        return builder.toString();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(JsonElement elem) {
        JsonElement copy = elem.clone(this);
        return list.add(copy);
    }

    @Override
    public boolean remove(Object obj) {
        return list.remove(obj);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return list.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends JsonElement> collection) {
        if (collection.isEmpty()) {
            return false;
        }
        for (JsonElement elem : collection) {
            JsonElement copy = elem.clone(this);
            list.add(copy);
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends JsonElement> collection) {
        if (collection.isEmpty()) {
            return false;
        }
        List<JsonElement> copies = new ArrayList<>(collection.size());
        for (JsonElement elem : collection) {
            JsonElement copy = elem.clone(this);
            copies.add(copy);
        }
        return list.addAll(index, copies);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return list.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return list.retainAll(collection);
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public JsonElement get(int index) {
        return list.get(index);
    }

    @Override
    public JsonElement set(int index, JsonElement element) {
        JsonElement copy = element.clone(this);
        return list.set(index, copy);
    }

    @Override
    public void add(int index, JsonElement element) {
        JsonElement copy = element.clone(this);
        list.add(index, copy);
    }

    @Override
    public JsonElement remove(int index) {
        return list.remove(index);
    }

    @Override
    public int indexOf(Object obj) {
        return list.indexOf(obj);
    }

    @Override
    public int lastIndexOf(Object obj) {
        return list.lastIndexOf(obj);
    }

    @Override
    public ListIterator<JsonElement> listIterator() {
        return Collections.unmodifiableList(list).listIterator();
    }

    @Override
    public ListIterator<JsonElement> listIterator(int index) {
        return Collections.unmodifiableList(list).listIterator(index);
    }

    @Override
    public List<JsonElement> subList(int fromIndex, int toIndex) {
        return Collections.unmodifiableList(list).subList(fromIndex, toIndex);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object obj) {
        return list.contains(obj);
    }

    @Override
    public Iterator<JsonElement> iterator() {
        return Collections.unmodifiableList(list).iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    /**
     * Returns a child element by its index.
     * @param index Index
     * @return Child element
     * @throws IndexOutOfBoundsException If the index goes out of bounds
     */
    public JsonElement getElement(int index) throws IndexOutOfBoundsException {
        return list.get(index);
    }

    /**
     * Makes a copy of the JSON element and appends this copy to the end of the array.
     * @param elem Element
     */
    public void addElement(JsonElement elem) {
        add(elem);
    }

    /**
     * Creates a child element of the 'null' type and adds it to the end of the array.
     */
    public void addNull() {
        list.add(new JsonNull(this));
    }

    /**
     * Creates a child element of the 'boolean' type and adds it to the end of the array.
     * @param value Boolean value
     */
    public void addBoolean(boolean value) {
        list.add(new JsonBoolean(this, value));
    }

    /**
     * Creates a child element of the 'number' type and adds it to the end of the array.
     * @param value Value of the number
     */
    public void addNumber(double value) {
        list.add(new JsonNumber(this, value));
    }

    /**
     * Creates a child element of the 'string' type and adds it to the end of the array.
     * @param value Value of the string
     */
    public void addString(String value) {
        list.add(new JsonString(this, value));
    }

    /**
     * Creates an empty array as a child of this array.
     * The child is then added to the end of the parent array.
     * @return The empty array created, so that it can be filled.
     */
    public JsonArray createArray() {
        JsonArray child = new JsonArray(this);
        list.add(child);
        return child;
    }

    /**
     * Creates an empty object as a child of this array.
     * The child is then added to the end of the array.
     * @return The empty object created, so that it can be filled.
     */
    public JsonObject createObject() {
        JsonObject child = new JsonObject(this);
        list.add(child);
        return child;
    }

    /**
     * Adds a child element (for internal use).
     * @param elem Element
     */
    void addChild(JsonElement elem) {
        assert elem.getParent() == this;
        list.add(elem);
    }

    @Override
    public JsonArray toJsonArray() {
        return this;
    }
}
