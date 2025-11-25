package org.yaml.snakeyaml.constructor;

import java.beans.IntrospectionException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

public class Constructor extends SafeConstructor {
   private final Map<Tag, Class<? extends Object>> typeTags;
   protected final Map<Class<? extends Object>, TypeDescription> typeDefinitions;

   public Constructor() {
      this(Object.class);
   }

   public Constructor(Class<? extends Object> theRoot) {
      this(new TypeDescription(checkRoot(theRoot)));
   }

   private static Class<? extends Object> checkRoot(Class<? extends Object> theRoot) {
      if (theRoot == null) {
         throw new NullPointerException("Root class must be provided.");
      } else {
         return theRoot;
      }
   }

   public Constructor(TypeDescription theRoot) {
      if (theRoot == null) {
         throw new NullPointerException("Root type must be provided.");
      } else {
         this.yamlConstructors.put((Object)null, new Constructor.ConstructYamlObject());
         if (!Object.class.equals(theRoot.getType())) {
            this.rootTag = new Tag(theRoot.getType());
         }

         this.typeTags = new HashMap();
         this.typeDefinitions = new HashMap();
         this.yamlClassConstructors.put(NodeId.scalar, new Constructor.ConstructScalar());
         this.yamlClassConstructors.put(NodeId.mapping, new Constructor.ConstructMapping());
         this.yamlClassConstructors.put(NodeId.sequence, new Constructor.ConstructSequence());
         this.addTypeDescription(theRoot);
      }
   }

   public Constructor(String theRoot) throws ClassNotFoundException {
      this(Class.forName(check(theRoot)));
   }

   private static final String check(String s) {
      if (s == null) {
         throw new NullPointerException("Root type must be provided.");
      } else if (s.trim().length() == 0) {
         throw new YAMLException("Root type must be provided.");
      } else {
         return s;
      }
   }

   public TypeDescription addTypeDescription(TypeDescription definition) {
      if (definition == null) {
         throw new NullPointerException("TypeDescription is required.");
      } else {
         Tag tag = definition.getTag();
         this.typeTags.put(tag, definition.getType());
         return (TypeDescription)this.typeDefinitions.put(definition.getType(), definition);
      }
   }

   protected Class<?> getClassForNode(Node node) {
      Class<? extends Object> classForTag = (Class)this.typeTags.get(node.getTag());
      if (classForTag == null) {
         String name = node.getTag().getClassName();

         Class cl;
         try {
            cl = this.getClassForName(name);
         } catch (ClassNotFoundException var6) {
            throw new YAMLException("Class not found: " + name);
         }

         this.typeTags.put(node.getTag(), cl);
         return cl;
      } else {
         return classForTag;
      }
   }

   protected Class<?> getClassForName(String name) throws ClassNotFoundException {
      return Class.forName(name);
   }

   protected class ConstructSequence implements Construct {
      public Object construct(Node node) {
         SequenceNode snode = (SequenceNode)node;
         if (Set.class.isAssignableFrom(node.getType())) {
            if (node.isTwoStepsConstruction()) {
               throw new YAMLException("Set cannot be recursive.");
            } else {
               return Constructor.this.constructSet(snode);
            }
         } else if (Collection.class.isAssignableFrom(node.getType())) {
            return node.isTwoStepsConstruction() ? Constructor.this.createDefaultList(snode.getValue().size()) : Constructor.this.constructSequence(snode);
         } else if (node.getType().isArray()) {
            return node.isTwoStepsConstruction() ? Constructor.this.createArray(node.getType(), snode.getValue().size()) : Constructor.this.constructArray(snode);
         } else {
            List<java.lang.reflect.Constructor<?>> possibleConstructors = new ArrayList(snode.getValue().size());
            java.lang.reflect.Constructor[] arr$ = node.getType().getConstructors();
            int len$ = arr$.length;

            int index;
            for(index = 0; index < len$; ++index) {
               java.lang.reflect.Constructor<?> constructor = arr$[index];
               if (snode.getValue().size() == constructor.getParameterTypes().length) {
                  possibleConstructors.add(constructor);
               }
            }

            if (!possibleConstructors.isEmpty()) {
               Iterator i$;
               if (possibleConstructors.size() == 1) {
                  Object[] argumentListx = new Object[snode.getValue().size()];
                  java.lang.reflect.Constructor<?> c = (java.lang.reflect.Constructor)possibleConstructors.get(0);
                  index = 0;

                  Node argumentNode;
                  for(i$ = snode.getValue().iterator(); i$.hasNext(); argumentListx[index++] = Constructor.this.constructObject(argumentNode)) {
                     argumentNode = (Node)i$.next();
                     Class<?> type = c.getParameterTypes()[index];
                     argumentNode.setType(type);
                  }

                  try {
                     return c.newInstance(argumentListx);
                  } catch (Exception var12) {
                     throw new YAMLException(var12);
                  }
               }

               List<Object> argumentList = Constructor.this.constructSequence(snode);
               Class<?>[] parameterTypes = new Class[argumentList.size()];
               index = 0;

               for(i$ = argumentList.iterator(); i$.hasNext(); ++index) {
                  Object parameter = i$.next();
                  parameterTypes[index] = parameter.getClass();
               }

               i$ = possibleConstructors.iterator();

               while(i$.hasNext()) {
                  java.lang.reflect.Constructor<?> cx = (java.lang.reflect.Constructor)i$.next();
                  Class<?>[] argTypes = cx.getParameterTypes();
                  boolean foundConstructor = true;

                  for(int i = 0; i < argTypes.length; ++i) {
                     if (!this.wrapIfPrimitive(argTypes[i]).isAssignableFrom(parameterTypes[i])) {
                        foundConstructor = false;
                        break;
                     }
                  }

                  if (foundConstructor) {
                     try {
                        return cx.newInstance(argumentList.toArray());
                     } catch (Exception var13) {
                        throw new YAMLException(var13);
                     }
                  }
               }
            }

            throw new YAMLException("No suitable constructor with " + String.valueOf(snode.getValue().size()) + " arguments found for " + node.getType());
         }
      }

      private final Class<? extends Object> wrapIfPrimitive(Class<?> clazz) {
         if (!clazz.isPrimitive()) {
            return clazz;
         } else if (clazz == Integer.TYPE) {
            return Integer.class;
         } else if (clazz == Float.TYPE) {
            return Float.class;
         } else if (clazz == Double.TYPE) {
            return Double.class;
         } else if (clazz == Boolean.TYPE) {
            return Boolean.class;
         } else if (clazz == Long.TYPE) {
            return Long.class;
         } else if (clazz == Character.TYPE) {
            return Character.class;
         } else if (clazz == Short.TYPE) {
            return Short.class;
         } else if (clazz == Byte.TYPE) {
            return Byte.class;
         } else {
            throw new YAMLException("Unexpected primitive " + clazz);
         }
      }

      public void construct2ndStep(Node node, Object object) {
         SequenceNode snode = (SequenceNode)node;
         if (List.class.isAssignableFrom(node.getType())) {
            List<Object> list = (List)object;
            Constructor.this.constructSequenceStep2(snode, list);
         } else {
            if (!node.getType().isArray()) {
               throw new YAMLException("Immutable objects cannot be recursive.");
            }

            Constructor.this.constructArrayStep2(snode, object);
         }

      }
   }

   protected class ConstructScalar extends AbstractConstruct {
      public Object construct(Node nnode) {
         ScalarNode node = (ScalarNode)nnode;
         Class<?> type = node.getType();
         Object result;
         if (!type.isPrimitive() && type != String.class && !Number.class.isAssignableFrom(type) && type != Boolean.class && !Date.class.isAssignableFrom(type) && type != Character.class && type != BigInteger.class && type != BigDecimal.class && !Enum.class.isAssignableFrom(type) && !Tag.BINARY.equals(node.getTag()) && !Calendar.class.isAssignableFrom(type)) {
            java.lang.reflect.Constructor<?>[] javaConstructors = type.getDeclaredConstructors();
            int oneArgCount = 0;
            java.lang.reflect.Constructor<?> javaConstructor = null;
            java.lang.reflect.Constructor[] arr$ = javaConstructors;
            int len$ = javaConstructors.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               java.lang.reflect.Constructor<?> c = arr$[i$];
               if (c.getParameterTypes().length == 1) {
                  ++oneArgCount;
                  javaConstructor = c;
               }
            }

            if (javaConstructor == null) {
               throw new YAMLException("No single argument constructor found for " + type);
            }

            Object argument;
            if (oneArgCount == 1) {
               argument = this.constructStandardJavaInstance(javaConstructor.getParameterTypes()[0], node);
            } else {
               argument = Constructor.this.constructScalar(node);

               try {
                  javaConstructor = type.getDeclaredConstructor(String.class);
               } catch (Exception var13) {
                  throw new YAMLException("Can't construct a java object for scalar " + node.getTag() + "; No String constructor found. Exception=" + var13.getMessage(), var13);
               }
            }

            try {
               javaConstructor.setAccessible(true);
               result = javaConstructor.newInstance(argument);
            } catch (Exception var12) {
               throw new ConstructorException((String)null, (Mark)null, "Can't construct a java object for scalar " + node.getTag() + "; exception=" + var12.getMessage(), node.getStartMark(), var12);
            }
         } else {
            result = this.constructStandardJavaInstance(type, node);
         }

         return result;
      }

      private Object constructStandardJavaInstance(Class type, ScalarNode node) {
         Object result;
         Construct doubleConstructor;
         if (type == String.class) {
            doubleConstructor = (Construct)Constructor.this.yamlConstructors.get(Tag.STR);
            result = doubleConstructor.construct(node);
         } else if (type != Boolean.class && type != Boolean.TYPE) {
            if (type != Character.class && type != Character.TYPE) {
               if (Date.class.isAssignableFrom(type)) {
                  doubleConstructor = (Construct)Constructor.this.yamlConstructors.get(Tag.TIMESTAMP);
                  Date date = (Date)doubleConstructor.construct(node);
                  if (type == Date.class) {
                     result = date;
                  } else {
                     try {
                        java.lang.reflect.Constructor<?> constr = type.getConstructor(Long.TYPE);
                        result = constr.newInstance(date.getTime());
                     } catch (RuntimeException var8) {
                        throw var8;
                     } catch (Exception var9) {
                        throw new YAMLException("Cannot construct: '" + type + "'");
                     }
                  }
               } else if (type != Float.class && type != Double.class && type != Float.TYPE && type != Double.TYPE && type != BigDecimal.class) {
                  if (type != Byte.class && type != Short.class && type != Integer.class && type != Long.class && type != BigInteger.class && type != Byte.TYPE && type != Short.TYPE && type != Integer.TYPE && type != Long.TYPE) {
                     if (Enum.class.isAssignableFrom(type)) {
                        String enumValueName = node.getValue();

                        try {
                           result = Enum.valueOf(type, enumValueName);
                        } catch (Exception var7) {
                           throw new YAMLException("Unable to find enum value '" + enumValueName + "' for enum class: " + type.getName());
                        }
                     } else if (Calendar.class.isAssignableFrom(type)) {
                        SafeConstructor.ConstructYamlTimestamp contr = new SafeConstructor.ConstructYamlTimestamp();
                        contr.construct(node);
                        result = contr.getCalendar();
                     } else if (Number.class.isAssignableFrom(type)) {
                        SafeConstructor.ConstructYamlNumber contrx = Constructor.this.new ConstructYamlNumber();
                        result = contrx.construct(node);
                     } else {
                        if (!Constructor.this.yamlConstructors.containsKey(node.getTag())) {
                           throw new YAMLException("Unsupported class: " + type);
                        }

                        result = ((Construct)Constructor.this.yamlConstructors.get(node.getTag())).construct(node);
                     }
                  } else {
                     doubleConstructor = (Construct)Constructor.this.yamlConstructors.get(Tag.INT);
                     result = doubleConstructor.construct(node);
                     if (type != Byte.class && type != Byte.TYPE) {
                        if (type != Short.class && type != Short.TYPE) {
                           if (type != Integer.class && type != Integer.TYPE) {
                              if (type != Long.class && type != Long.TYPE) {
                                 result = new BigInteger(result.toString());
                              } else {
                                 result = Long.valueOf(result.toString());
                              }
                           } else {
                              result = Integer.parseInt(result.toString());
                           }
                        } else {
                           result = Short.valueOf(result.toString());
                        }
                     } else {
                        result = Byte.valueOf(result.toString());
                     }
                  }
               } else if (type == BigDecimal.class) {
                  result = new BigDecimal(node.getValue());
               } else {
                  doubleConstructor = (Construct)Constructor.this.yamlConstructors.get(Tag.FLOAT);
                  result = doubleConstructor.construct(node);
                  if (type == Float.class || type == Float.TYPE) {
                     result = new Float((Double)result);
                  }
               }
            } else {
               doubleConstructor = (Construct)Constructor.this.yamlConstructors.get(Tag.STR);
               String ch = (String)doubleConstructor.construct(node);
               if (ch.length() == 0) {
                  result = null;
               } else {
                  if (ch.length() != 1) {
                     throw new YAMLException("Invalid node Character: '" + ch + "'; length: " + ch.length());
                  }

                  result = ch.charAt(0);
               }
            }
         } else {
            doubleConstructor = (Construct)Constructor.this.yamlConstructors.get(Tag.BOOL);
            result = doubleConstructor.construct(node);
         }

         return result;
      }
   }

   protected class ConstructYamlObject implements Construct {
      private Construct getConstructor(Node node) {
         Class<?> cl = Constructor.this.getClassForNode(node);
         node.setType(cl);
         Construct constructor = (Construct)Constructor.this.yamlClassConstructors.get(node.getNodeId());
         return constructor;
      }

      public Object construct(Node node) {
         Object result = null;

         try {
            result = this.getConstructor(node).construct(node);
            return result;
         } catch (ConstructorException var4) {
            throw var4;
         } catch (Exception var5) {
            throw new ConstructorException((String)null, (Mark)null, "Can't construct a java object for " + node.getTag() + "; exception=" + var5.getMessage(), node.getStartMark(), var5);
         }
      }

      public void construct2ndStep(Node node, Object object) {
         try {
            this.getConstructor(node).construct2ndStep(node, object);
         } catch (Exception var4) {
            throw new ConstructorException((String)null, (Mark)null, "Can't construct a second step for a java object for " + node.getTag() + "; exception=" + var4.getMessage(), node.getStartMark(), var4);
         }
      }
   }

   protected class ConstructMapping implements Construct {
      public Object construct(Node node) {
         MappingNode mnode = (MappingNode)node;
         if (Properties.class.isAssignableFrom(node.getType())) {
            Properties properties = new Properties();
            if (!node.isTwoStepsConstruction()) {
               Constructor.this.constructMapping2ndStep(mnode, properties);
               return properties;
            } else {
               throw new YAMLException("Properties must not be recursive.");
            }
         } else if (SortedMap.class.isAssignableFrom(node.getType())) {
            SortedMap<Object, Object> map = new TreeMap();
            if (!node.isTwoStepsConstruction()) {
               Constructor.this.constructMapping2ndStep(mnode, map);
            }

            return map;
         } else if (Map.class.isAssignableFrom(node.getType())) {
            return node.isTwoStepsConstruction() ? Constructor.this.createDefaultMap() : Constructor.this.constructMapping(mnode);
         } else if (SortedSet.class.isAssignableFrom(node.getType())) {
            SortedSet<Object> set = new TreeSet();
            Constructor.this.constructSet2ndStep(mnode, set);
            return set;
         } else if (Collection.class.isAssignableFrom(node.getType())) {
            return node.isTwoStepsConstruction() ? Constructor.this.createDefaultSet() : Constructor.this.constructSet(mnode);
         } else {
            return node.isTwoStepsConstruction() ? this.createEmptyJavaBean(mnode) : this.constructJavaBean2ndStep(mnode, this.createEmptyJavaBean(mnode));
         }
      }

      public void construct2ndStep(Node node, Object object) {
         if (Map.class.isAssignableFrom(node.getType())) {
            Constructor.this.constructMapping2ndStep((MappingNode)node, (Map)object);
         } else if (Set.class.isAssignableFrom(node.getType())) {
            Constructor.this.constructSet2ndStep((MappingNode)node, (Set)object);
         } else {
            this.constructJavaBean2ndStep((MappingNode)node, object);
         }

      }

      protected Object createEmptyJavaBean(MappingNode node) {
         try {
            java.lang.reflect.Constructor<?> c = node.getType().getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
         } catch (Exception var3) {
            throw new YAMLException(var3);
         }
      }

      protected Object constructJavaBean2ndStep(MappingNode node, Object object) {
         Constructor.this.flattenMapping(node);
         Class<? extends Object> beanType = node.getType();
         List<NodeTuple> nodeValue = node.getValue();
         Iterator i$ = nodeValue.iterator();

         while(i$.hasNext()) {
            NodeTuple tuple = (NodeTuple)i$.next();
            if (!(tuple.getKeyNode() instanceof ScalarNode)) {
               throw new YAMLException("Keys must be scalars but found: " + tuple.getKeyNode());
            }

            ScalarNode keyNode = (ScalarNode)tuple.getKeyNode();
            Node valueNode = tuple.getValueNode();
            keyNode.setType(String.class);
            String key = (String)Constructor.this.constructObject(keyNode);

            try {
               Property property = this.getProperty(beanType, key);
               valueNode.setType(property.getType());
               TypeDescription memberDescription = (TypeDescription)Constructor.this.typeDefinitions.get(beanType);
               boolean typeDetected = false;
               Class ketType;
               MappingNode mnodex;
               if (memberDescription != null) {
                  switch(valueNode.getNodeId()) {
                  case sequence:
                     SequenceNode snode = (SequenceNode)valueNode;
                     ketType = memberDescription.getListPropertyType(key);
                     if (ketType != null) {
                        snode.setListType(ketType);
                        typeDetected = true;
                     } else if (property.getType().isArray()) {
                        snode.setListType(property.getType().getComponentType());
                        typeDetected = true;
                     }
                     break;
                  case mapping:
                     mnodex = (MappingNode)valueNode;
                     Class<? extends Object> keyType = memberDescription.getMapKeyType(key);
                     if (keyType != null) {
                        mnodex.setTypes(keyType, memberDescription.getMapValueType(key));
                        typeDetected = true;
                     }
                  }
               }

               if (!typeDetected && valueNode.getNodeId() != NodeId.scalar) {
                  Class<?>[] arguments = property.getActualTypeArguments();
                  if (arguments != null && arguments.length > 0) {
                     if (valueNode.getNodeId() == NodeId.sequence) {
                        ketType = arguments[0];
                        SequenceNode snodex = (SequenceNode)valueNode;
                        snodex.setListType(ketType);
                     } else if (valueNode.getTag().equals(Tag.SET)) {
                        ketType = arguments[0];
                        mnodex = (MappingNode)valueNode;
                        mnodex.setOnlyKeyType(ketType);
                        mnodex.setUseClassConstructor(true);
                     } else if (property.getType().isAssignableFrom(Map.class)) {
                        ketType = arguments[0];
                        Class<?> valueType = arguments[1];
                        MappingNode mnode = (MappingNode)valueNode;
                        mnode.setTypes(ketType, valueType);
                        mnode.setUseClassConstructor(true);
                     }
                  }
               }

               Object value = Constructor.this.constructObject(valueNode);
               if ((property.getType() == Float.TYPE || property.getType() == Float.class) && value instanceof Double) {
                  value = ((Double)value).floatValue();
               }

               if (property.getType() == String.class && Tag.BINARY.equals(valueNode.getTag()) && value instanceof byte[]) {
                  value = new String((byte[])((byte[])value));
               }

               property.set(object, value);
            } catch (Exception var17) {
               throw new ConstructorException("Cannot create property=" + key + " for JavaBean=" + object, node.getStartMark(), var17.getMessage(), valueNode.getStartMark(), var17);
            }
         }

         return object;
      }

      protected Property getProperty(Class<? extends Object> type, String name) throws IntrospectionException {
         return Constructor.this.getPropertyUtils().getProperty(type, name);
      }
   }
}
