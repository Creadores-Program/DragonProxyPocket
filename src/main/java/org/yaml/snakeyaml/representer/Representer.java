package org.yaml.snakeyaml.representer;

import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

public class Representer extends SafeRepresenter {
   public Representer() {
      this.representers.put((Object)null, new Representer.RepresentJavaBean());
   }

   protected MappingNode representJavaBean(Set<Property> properties, Object javaBean) {
      List<NodeTuple> value = new ArrayList(properties.size());
      Tag customTag = (Tag)this.classTags.get(javaBean.getClass());
      Tag tag = customTag != null ? customTag : new Tag(javaBean.getClass());
      MappingNode node = new MappingNode(tag, value, (Boolean)null);
      this.representedObjects.put(javaBean, node);
      boolean bestStyle = true;
      Iterator i$ = properties.iterator();

      while(true) {
         NodeTuple tuple;
         do {
            if (!i$.hasNext()) {
               if (this.defaultFlowStyle != DumperOptions.FlowStyle.AUTO) {
                  node.setFlowStyle(this.defaultFlowStyle.getStyleBoolean());
               } else {
                  node.setFlowStyle(bestStyle);
               }

               return node;
            }

            Property property = (Property)i$.next();
            Object memberValue = property.get(javaBean);
            Tag customPropertyTag = memberValue == null ? null : (Tag)this.classTags.get(memberValue.getClass());
            tuple = this.representJavaBeanProperty(javaBean, property, memberValue, customPropertyTag);
         } while(tuple == null);

         if (((ScalarNode)tuple.getKeyNode()).getStyle() != null) {
            bestStyle = false;
         }

         Node nodeValue = tuple.getValueNode();
         if (!(nodeValue instanceof ScalarNode) || ((ScalarNode)nodeValue).getStyle() != null) {
            bestStyle = false;
         }

         value.add(tuple);
      }
   }

   protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
      ScalarNode nodeKey = (ScalarNode)this.representData(property.getName());
      boolean hasAlias = this.representedObjects.containsKey(propertyValue);
      Node nodeValue = this.representData(propertyValue);
      if (propertyValue != null && !hasAlias) {
         NodeId nodeId = nodeValue.getNodeId();
         if (customTag == null) {
            if (nodeId == NodeId.scalar) {
               if (propertyValue instanceof Enum) {
                  nodeValue.setTag(Tag.STR);
               }
            } else {
               if (nodeId == NodeId.mapping && property.getType() == propertyValue.getClass() && !(propertyValue instanceof Map) && !nodeValue.getTag().equals(Tag.SET)) {
                  nodeValue.setTag(Tag.MAP);
               }

               this.checkGlobalTag(property, nodeValue, propertyValue);
            }
         }
      }

      return new NodeTuple(nodeKey, nodeValue);
   }

   protected void checkGlobalTag(Property property, Node node, Object object) {
      if (!object.getClass().isArray() || !object.getClass().getComponentType().isPrimitive()) {
         Class<?>[] arguments = property.getActualTypeArguments();
         if (arguments != null) {
            Class t;
            Iterator i$;
            Iterator i$;
            if (node.getNodeId() == NodeId.sequence) {
               t = arguments[0];
               SequenceNode snode = (SequenceNode)node;
               Object memberList;
               if (object.getClass().isArray()) {
                  memberList = Arrays.asList((Object[])((Object[])object));
               } else {
                  memberList = (Iterable)object;
               }

               i$ = ((Iterable)memberList).iterator();
               i$ = snode.getValue().iterator();

               while(i$.hasNext()) {
                  Node childNode = (Node)i$.next();
                  Object member = i$.next();
                  if (member != null && t.equals(member.getClass()) && childNode.getNodeId() == NodeId.mapping) {
                     childNode.setTag(Tag.MAP);
                  }
               }
            } else if (object instanceof Set) {
               t = arguments[0];
               MappingNode mnode = (MappingNode)node;
               Iterator<NodeTuple> iter = mnode.getValue().iterator();
               Set<?> set = (Set)object;
               i$ = set.iterator();

               while(i$.hasNext()) {
                  Object member = i$.next();
                  NodeTuple tuple = (NodeTuple)iter.next();
                  Node keyNode = tuple.getKeyNode();
                  if (t.equals(member.getClass()) && keyNode.getNodeId() == NodeId.mapping) {
                     keyNode.setTag(Tag.MAP);
                  }
               }
            } else if (object instanceof Map) {
               t = arguments[0];
               Class<?> valueType = arguments[1];
               MappingNode mnode = (MappingNode)node;
               i$ = mnode.getValue().iterator();

               while(i$.hasNext()) {
                  NodeTuple tuple = (NodeTuple)i$.next();
                  this.resetTag(t, tuple.getKeyNode());
                  this.resetTag(valueType, tuple.getValueNode());
               }
            }
         }

      }
   }

   private void resetTag(Class<? extends Object> type, Node node) {
      Tag tag = node.getTag();
      if (tag.matches(type)) {
         if (Enum.class.isAssignableFrom(type)) {
            node.setTag(Tag.STR);
         } else {
            node.setTag(Tag.MAP);
         }
      }

   }

   protected Set<Property> getProperties(Class<? extends Object> type) throws IntrospectionException {
      return this.getPropertyUtils().getProperties(type);
   }

   protected class RepresentJavaBean implements Represent {
      public Node representData(Object data) {
         try {
            return Representer.this.representJavaBean(Representer.this.getProperties(data.getClass()), data);
         } catch (IntrospectionException var3) {
            throw new YAMLException(var3);
         }
      }
   }
}
