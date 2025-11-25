package org.yaml.snakeyaml.constructor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

public abstract class BaseConstructor {
   protected final Map<NodeId, Construct> yamlClassConstructors = new EnumMap(NodeId.class);
   protected final Map<Tag, Construct> yamlConstructors = new HashMap();
   protected final Map<String, Construct> yamlMultiConstructors = new HashMap();
   private Composer composer;
   private final Map<Node, Object> constructedObjects = new HashMap();
   private final Set<Node> recursiveObjects = new HashSet();
   private final ArrayList<BaseConstructor.RecursiveTuple<Map<Object, Object>, BaseConstructor.RecursiveTuple<Object, Object>>> maps2fill = new ArrayList();
   private final ArrayList<BaseConstructor.RecursiveTuple<Set<Object>, Object>> sets2fill = new ArrayList();
   protected Tag rootTag = null;
   private PropertyUtils propertyUtils;
   private boolean explicitPropertyUtils = false;

   public void setComposer(Composer composer) {
      this.composer = composer;
   }

   public boolean checkData() {
      return this.composer.checkNode();
   }

   public Object getData() {
      this.composer.checkNode();
      Node node = this.composer.getNode();
      if (this.rootTag != null) {
         node.setTag(this.rootTag);
      }

      return this.constructDocument(node);
   }

   public Object getSingleData(Class<?> type) {
      Node node = this.composer.getSingleNode();
      if (node != null) {
         if (Object.class != type) {
            node.setTag(new Tag(type));
         } else if (this.rootTag != null) {
            node.setTag(this.rootTag);
         }

         return this.constructDocument(node);
      } else {
         return null;
      }
   }

   private Object constructDocument(Node node) {
      Object data = this.constructObject(node);
      this.fillRecursive();
      this.constructedObjects.clear();
      this.recursiveObjects.clear();
      return data;
   }

   private void fillRecursive() {
      Iterator i$;
      BaseConstructor.RecursiveTuple value;
      if (!this.maps2fill.isEmpty()) {
         i$ = this.maps2fill.iterator();

         while(i$.hasNext()) {
            value = (BaseConstructor.RecursiveTuple)i$.next();
            BaseConstructor.RecursiveTuple<Object, Object> key_value = (BaseConstructor.RecursiveTuple)value._2();
            ((Map)value._1()).put(key_value._1(), key_value._2());
         }

         this.maps2fill.clear();
      }

      if (!this.sets2fill.isEmpty()) {
         i$ = this.sets2fill.iterator();

         while(i$.hasNext()) {
            value = (BaseConstructor.RecursiveTuple)i$.next();
            ((Set)value._1()).add(value._2());
         }

         this.sets2fill.clear();
      }

   }

   protected Object constructObject(Node node) {
      if (this.constructedObjects.containsKey(node)) {
         return this.constructedObjects.get(node);
      } else if (this.recursiveObjects.contains(node)) {
         throw new ConstructorException((String)null, (Mark)null, "found unconstructable recursive node", node.getStartMark());
      } else {
         this.recursiveObjects.add(node);
         Construct constructor = this.getConstructor(node);
         Object data = constructor.construct(node);
         this.constructedObjects.put(node, data);
         this.recursiveObjects.remove(node);
         if (node.isTwoStepsConstruction()) {
            constructor.construct2ndStep(node, data);
         }

         return data;
      }
   }

   protected Construct getConstructor(Node node) {
      if (node.useClassConstructor()) {
         return (Construct)this.yamlClassConstructors.get(node.getNodeId());
      } else {
         Construct constructor = (Construct)this.yamlConstructors.get(node.getTag());
         if (constructor == null) {
            Iterator i$ = this.yamlMultiConstructors.keySet().iterator();

            String prefix;
            do {
               if (!i$.hasNext()) {
                  return (Construct)this.yamlConstructors.get((Object)null);
               }

               prefix = (String)i$.next();
            } while(!node.getTag().startsWith(prefix));

            return (Construct)this.yamlMultiConstructors.get(prefix);
         } else {
            return constructor;
         }
      }
   }

   protected Object constructScalar(ScalarNode node) {
      return node.getValue();
   }

   protected List<Object> createDefaultList(int initSize) {
      return new ArrayList(initSize);
   }

   protected Set<Object> createDefaultSet(int initSize) {
      return new LinkedHashSet(initSize);
   }

   protected Object createArray(Class<?> type, int size) {
      return Array.newInstance(type.getComponentType(), size);
   }

   protected List<? extends Object> constructSequence(SequenceNode node) {
      List result;
      if (List.class.isAssignableFrom(node.getType()) && !node.getType().isInterface()) {
         try {
            result = (List)node.getType().newInstance();
         } catch (Exception var4) {
            throw new YAMLException(var4);
         }
      } else {
         result = this.createDefaultList(node.getValue().size());
      }

      this.constructSequenceStep2(node, result);
      return result;
   }

   protected Set<? extends Object> constructSet(SequenceNode node) {
      Set result;
      if (!node.getType().isInterface()) {
         try {
            result = (Set)node.getType().newInstance();
         } catch (Exception var4) {
            throw new YAMLException(var4);
         }
      } else {
         result = this.createDefaultSet(node.getValue().size());
      }

      this.constructSequenceStep2(node, result);
      return result;
   }

   protected Object constructArray(SequenceNode node) {
      return this.constructArrayStep2(node, this.createArray(node.getType(), node.getValue().size()));
   }

   protected void constructSequenceStep2(SequenceNode node, Collection<Object> collection) {
      Iterator i$ = node.getValue().iterator();

      while(i$.hasNext()) {
         Node child = (Node)i$.next();
         collection.add(this.constructObject(child));
      }

   }

   protected Object constructArrayStep2(SequenceNode node, Object array) {
      Class<?> componentType = node.getType().getComponentType();
      int index = 0;

      for(Iterator i$ = node.getValue().iterator(); i$.hasNext(); ++index) {
         Node child = (Node)i$.next();
         if (child.getType() == Object.class) {
            child.setType(componentType);
         }

         Object value = this.constructObject(child);
         if (componentType.isPrimitive()) {
            if (value == null) {
               throw new NullPointerException("Unable to construct element value for " + child);
            }

            if (Byte.TYPE.equals(componentType)) {
               Array.setByte(array, index, ((Number)value).byteValue());
            } else if (Short.TYPE.equals(componentType)) {
               Array.setShort(array, index, ((Number)value).shortValue());
            } else if (Integer.TYPE.equals(componentType)) {
               Array.setInt(array, index, ((Number)value).intValue());
            } else if (Long.TYPE.equals(componentType)) {
               Array.setLong(array, index, ((Number)value).longValue());
            } else if (Float.TYPE.equals(componentType)) {
               Array.setFloat(array, index, ((Number)value).floatValue());
            } else if (Double.TYPE.equals(componentType)) {
               Array.setDouble(array, index, ((Number)value).doubleValue());
            } else if (Character.TYPE.equals(componentType)) {
               Array.setChar(array, index, (Character)value);
            } else {
               if (!Boolean.TYPE.equals(componentType)) {
                  throw new YAMLException("unexpected primitive type");
               }

               Array.setBoolean(array, index, (Boolean)value);
            }
         } else {
            Array.set(array, index, value);
         }
      }

      return array;
   }

   protected Map<Object, Object> createDefaultMap() {
      return new LinkedHashMap();
   }

   protected Set<Object> createDefaultSet() {
      return new LinkedHashSet();
   }

   protected Set<Object> constructSet(MappingNode node) {
      Set<Object> set = this.createDefaultSet();
      this.constructSet2ndStep(node, set);
      return set;
   }

   protected Map<Object, Object> constructMapping(MappingNode node) {
      Map<Object, Object> mapping = this.createDefaultMap();
      this.constructMapping2ndStep(node, mapping);
      return mapping;
   }

   protected void constructMapping2ndStep(MappingNode node, Map<Object, Object> mapping) {
      List<NodeTuple> nodeValue = node.getValue();
      Iterator i$ = nodeValue.iterator();

      while(i$.hasNext()) {
         NodeTuple tuple = (NodeTuple)i$.next();
         Node keyNode = tuple.getKeyNode();
         Node valueNode = tuple.getValueNode();
         Object key = this.constructObject(keyNode);
         if (key != null) {
            try {
               key.hashCode();
            } catch (Exception var10) {
               throw new ConstructorException("while constructing a mapping", node.getStartMark(), "found unacceptable key " + key, tuple.getKeyNode().getStartMark(), var10);
            }
         }

         Object value = this.constructObject(valueNode);
         if (keyNode.isTwoStepsConstruction()) {
            this.maps2fill.add(0, new BaseConstructor.RecursiveTuple(mapping, new BaseConstructor.RecursiveTuple(key, value)));
         } else {
            mapping.put(key, value);
         }
      }

   }

   protected void constructSet2ndStep(MappingNode node, Set<Object> set) {
      List<NodeTuple> nodeValue = node.getValue();
      Iterator i$ = nodeValue.iterator();

      while(i$.hasNext()) {
         NodeTuple tuple = (NodeTuple)i$.next();
         Node keyNode = tuple.getKeyNode();
         Object key = this.constructObject(keyNode);
         if (key != null) {
            try {
               key.hashCode();
            } catch (Exception var9) {
               throw new ConstructorException("while constructing a Set", node.getStartMark(), "found unacceptable key " + key, tuple.getKeyNode().getStartMark(), var9);
            }
         }

         if (keyNode.isTwoStepsConstruction()) {
            this.sets2fill.add(0, new BaseConstructor.RecursiveTuple(set, key));
         } else {
            set.add(key);
         }
      }

   }

   public void setPropertyUtils(PropertyUtils propertyUtils) {
      this.propertyUtils = propertyUtils;
      this.explicitPropertyUtils = true;
   }

   public final PropertyUtils getPropertyUtils() {
      if (this.propertyUtils == null) {
         this.propertyUtils = new PropertyUtils();
      }

      return this.propertyUtils;
   }

   public final boolean isExplicitPropertyUtils() {
      return this.explicitPropertyUtils;
   }

   private static class RecursiveTuple<T, K> {
      private final T _1;
      private final K _2;

      public RecursiveTuple(T _1, K _2) {
         this._1 = _1;
         this._2 = _2;
      }

      public K _2() {
         return this._2;
      }

      public T _1() {
         return this._1;
      }
   }
}
