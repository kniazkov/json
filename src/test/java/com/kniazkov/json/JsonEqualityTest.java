/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;

public class JsonEqualityTest {
    private void equal(Object a, Object b) {
        Assert.assertTrue(a.equals(b));
        Assert.assertTrue(b.equals(a));
        Assert.assertEquals(a.hashCode(),b.hashCode());
    }

    @Test
    public void scalarEqualityIgnoresParentsButNotJsonTypes() {
        JsonElement[] values={JsonNull.INSTANCE,new JsonString("text"),JsonBoolean.of(true),JsonNumber.of(7)};
        JsonArray parent=new JsonArray();
        for(JsonElement value:values) {
            parent.add(value);
            JsonElement child=parent.get(parent.size()-1);
            equal(value,child); equal(child,child.clone()); equal(value,child.clone());
            Assert.assertFalse(value.equals(null));
            Assert.assertEquals(1,new HashSet<>(Arrays.asList(value,child,child.clone())).size());
        }
        Assert.assertNotEquals(new JsonString("true"),JsonBoolean.of(true));
        Assert.assertNotEquals(JsonBoolean.of(true),JsonNumber.of(1));
        Assert.assertNotEquals(JsonNull.INSTANCE,new JsonString("null"));
        for(Object[] pair:new Object[][]{{new JsonString("x"),"x"},{JsonNumber.of(1),1},{JsonBoolean.of(true),true}}) {
            Assert.assertFalse(pair[0].equals(pair[1])); Assert.assertFalse(pair[1].equals(pair[0]));
        }
    }

    @Test
    public void numbersCompareExactlyAcrossRepresentations() throws Exception {
        JsonElement a=Json.parse("1"), b=Json.parse("1.00"), c=Json.parse("1e0");
        equal(a,b);equal(b,c);equal(a,c);
        equal(JsonNumber.of(0),JsonNumber.of(new BigDecimal("-0.000")));
        equal(JsonNumber.of(new BigInteger("123456789012345678901234567890")),
                JsonNumber.of(new BigDecimal("123456789012345678901234567890.000")));
        Assert.assertNotEquals(Json.parse("9007199254740992"),Json.parse("9007199254740993"));
        Assert.assertNotEquals(Json.parse("0.10000000000000000001"),Json.parse("0.1"));
        Assert.assertEquals(1,new HashSet<>(Arrays.asList(a,b,c)).size());
    }

    @Test
    public void canonicalNumericHashSupportsExtremeScalesWithoutOverflow() {
        equal(JsonNumber.of(new BigDecimal(BigInteger.TEN,Integer.MIN_VALUE)),
                JsonNumber.of(new BigDecimal(BigInteger.valueOf(100),Integer.MIN_VALUE+1)));
        equal(JsonNumber.of(new BigDecimal(BigInteger.TEN,Integer.MAX_VALUE)),
                JsonNumber.of(new BigDecimal(BigInteger.ONE,Integer.MAX_VALUE-1)));
        equal(JsonNumber.of(new BigDecimal(BigInteger.ZERO,Integer.MIN_VALUE)),JsonNumber.of(0));
    }

    @Test
    public void specialNumbersHaveReflexiveValueEquality() {
        equal(new JsonNaN(),new JsonNaN());
        equal(new JsonInfinity(false),JsonNumber.of(Double.POSITIVE_INFINITY));
        equal(new JsonInfinity(true),JsonNumber.of(Float.NEGATIVE_INFINITY));
        Assert.assertNotEquals(new JsonNaN(),new JsonInfinity(false));
        Assert.assertNotEquals(new JsonInfinity(true),new JsonInfinity(false));
        Assert.assertNotEquals(new JsonInfinity(false),JsonNumber.of(new BigDecimal("1e400")));
        Assert.assertNotEquals(new JsonNaN(),Double.NaN);
    }

    @Test
    public void arraysFollowTheListContractIncludingStandardLists() throws Exception {
        equal(new JsonArray(),new ArrayList<>());
        JsonArray array=Json.parse("[1,{x:true},[null,'a']]").arrayValueOrThrow();
        List<JsonElement> standard=new ArrayList<>(array);
        equal(array,standard); equal(array,new LinkedList<>(standard));
        equal(array,array.clone());
        List<Object> nested=Arrays.asList(JsonNumber.of(1.0),Map.of("x",JsonBoolean.of(true)),
                Arrays.asList(JsonNull.INSTANCE,new JsonString("a")));
        equal(array,nested);
        Assert.assertNotEquals(array,Json.parse("[{x:true},1,[null,'a']]"));
        Assert.assertNotEquals(array,new HashSet<>(standard));
        Assert.assertNotEquals(Json.parse("[null]"),Collections.singletonList(null));
        Assert.assertNotEquals(Json.parse("[1]"),List.of(1));
    }

    @Test
    public void objectsFollowTheMapContractIgnoringMemberOrder() throws Exception {
        equal(new JsonObject(),new HashMap<>());
        JsonObject object=Json.parse("{a:1,b:[true],c:null}").objectValueOrThrow();
        JsonObject reordered=Json.parse("{c:null,b:[true],a:1.0}").objectValueOrThrow();
        equal(object,reordered); equal(object,new HashMap<>(object)); equal(object,new TreeMap<>(object));
        equal(object,Map.of("a",JsonNumber.of(1),"b",List.of(JsonBoolean.of(true)),"c",JsonNull.INSTANCE));
        Assert.assertNotEquals(object,Json.parse("{a:1,b:[false],c:null}"));
        Assert.assertNotEquals(Json.parse("{a:null}"),Collections.singletonMap("a",null));
        Assert.assertNotEquals(Json.parse("{a:null}"),Collections.singletonMap("b",null));
    }

    @Test
    public void listLookupRemovalAndBulkOperationsWorkAfterCloning() throws Exception {
        JsonArray array=new JsonArray();JsonNumber number=JsonNumber.of(1);
        array.add(number);array.add(JsonNumber.of(2));array.add(number);
        Assert.assertNotSame(number,array.get(0));Assert.assertTrue(array.contains(number));
        Assert.assertEquals(0,array.indexOf(JsonNumber.of(1.0)));Assert.assertEquals(2,array.lastIndexOf(number));
        Assert.assertTrue(array.containsAll(List.of(number,JsonNumber.of(2))));
        Assert.assertTrue(array.remove(number));
        Assert.assertTrue(array.removeAll(List.of(JsonNumber.of(1))));
        equal(array,List.of(JsonNumber.of(2)));
        array.add(Json.parse("{x:[3]}"));array.addNull();
        Assert.assertTrue(array.retainAll(List.of(Json.parse("{x:[3]}"))));
        Assert.assertEquals(1,array.size());
    }

    @Test
    public void mapDefaultsAndViewsUseValueEquality() throws Exception {
        JsonObject object=Json.parse("{a:1,b:{x:2},c:3}").objectValueOrThrow();
        Assert.assertTrue(object.containsValue(JsonNumber.of(1.0)));
        Assert.assertTrue(object.replace("a",JsonNumber.of(1),JsonNumber.of(4)));
        Assert.assertTrue(object.remove("a",JsonNumber.of(4.0)));
        Assert.assertTrue(object.entrySet().contains(new AbstractMap.SimpleEntry<>("b",Json.parse("{x:2}"))));
        Assert.assertTrue(object.entrySet().remove(new AbstractMap.SimpleEntry<>("b",Json.parse("{x:2.0}"))));
        Assert.assertTrue(object.values().remove(JsonNumber.of(3)));Assert.assertTrue(object.isEmpty());
        object.put("x",Json.parse("[1]"));
        equal(object.entrySet(),new HashMap<>(object).entrySet());
    }

    @Test
    public void arrayDefaultMutationsKeepParentsAndCloneNewValues() {
        JsonArray array=new JsonArray();array.addNumber(3);array.addNumber(1);array.addNumber(2);
        array.sort(Comparator.comparingInt(JsonElement::intValue));
        Assert.assertEquals("[1, 2, 3]",array.toString());
        JsonNumber source=JsonNumber.of(7);
        array.replaceAll(value->source);
        for(JsonElement value:array) { Assert.assertSame(array,value.parent());Assert.assertNotSame(source,value); }
        Assert.assertNotSame(array.get(0),array.get(1));
        Assert.assertTrue(array.removeIf(value->value.equals(JsonNumber.of(7.0))));
        Assert.assertTrue(array.isEmpty());
        Assert.assertThrows(NullPointerException.class,()->array.replaceAll(null));
        Assert.assertThrows(NullPointerException.class,()->array.removeIf(null));
        array.addNumber(1);
        Assert.assertThrows(NullPointerException.class,()->array.replaceAll(value->null));
        Assert.assertEquals("[1]",array.toString());
        array.replaceAll(value->array); // finite snapshot; no ownership cycle
        Assert.assertEquals("[[1]]",array.toString());
        Assert.assertSame(array,array.get(0).parent());
    }

    @Test
    public void arrayIteratorsAndSubListsRemainReadOnlyViews() {
        JsonArray array=new JsonArray();array.addNumber(1);array.addNumber(2);
        Iterator<JsonElement> iterator=array.iterator();iterator.next();
        Assert.assertThrows(UnsupportedOperationException.class,iterator::remove);
        ListIterator<JsonElement> listIterator=array.listIterator();listIterator.next();
        Assert.assertThrows(UnsupportedOperationException.class,()->listIterator.set(JsonNumber.of(3)));
        Assert.assertThrows(UnsupportedOperationException.class,()->listIterator.add(JsonNumber.of(3)));
        List<JsonElement> view=array.subList(0,1);
        Assert.assertThrows(UnsupportedOperationException.class,()->view.removeIf(value->true));
        Assert.assertThrows(UnsupportedOperationException.class,()->view.replaceAll(value->value));
        Assert.assertThrows(UnsupportedOperationException.class,()->view.sort(Comparator.comparingInt(JsonElement::intValue)));
        array.set(0,JsonNumber.of(9));Assert.assertEquals(9,view.get(0).intValue());
    }

    @Test
    public void selfBulkInsertionUsesASnapshotAndValidatesEmptyInsertionIndex() {
        JsonArray array=new JsonArray();array.addNumber(1);array.addNumber(2);
        Assert.assertTrue(array.addAll(array));Assert.assertEquals("[1, 2, 1, 2]",array.toString());
        Assert.assertNotSame(array.get(0),array.get(2));
        Assert.assertThrows(IndexOutOfBoundsException.class,()->array.addAll(99,List.of()));
        Assert.assertFalse(array.addAll(array.size(),List.of()));
    }

    @Test
    public void deepEqualityAndHashingRemainStackSafe() throws Exception {
        int depth=20_000;
        String input="{\"x\":[".repeat(depth)+"1"+"]}".repeat(depth);
        JsonElement a=Json.parse(input),b=a.clone(),c=Json.parse(input.replace("1","1.0"));
        equal(a,b);equal(b,c);equal(a,c);
        Set<JsonElement> set=new HashSet<>();set.add(a);Assert.assertTrue(set.contains(b));
        JsonElement different=Json.parse(input.replace("1","2"));Assert.assertNotEquals(a,different);
        Assert.assertEquals(a.hashCode(),a.hashCode());
    }

    @Test
    public void deepComparisonAlsoWalksForeignContainersIteratively() throws Exception {
        int depth=20_000;
        JsonElement json=Json.parse("[".repeat(depth)+"1"+"]".repeat(depth));
        Object standard=JsonNumber.of(1.0);
        for(int i=0;i<depth;i++) { standard=Collections.singletonList(standard); }
        Assert.assertTrue(json.equals(standard));
        JsonArray mutable=Json.parse("[1]").arrayValueOrThrow();
        int originalHash=mutable.hashCode();
        mutable.set(0,JsonNumber.of(2));
        Assert.assertNotEquals(originalHash,mutable.hashCode());
        equal(mutable,List.of(JsonNumber.of(2)));
    }
}
