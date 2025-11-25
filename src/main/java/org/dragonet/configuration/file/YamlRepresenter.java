package org.dragonet.configuration.file;

import java.util.LinkedHashMap;
import java.util.Map;
import org.dragonet.configuration.ConfigurationSection;
import org.dragonet.configuration.serialization.ConfigurationSerializable;
import org.dragonet.configuration.serialization.ConfigurationSerialization;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.representer.SafeRepresenter;

public class YamlRepresenter extends Representer {
   public YamlRepresenter() {
      this.multiRepresenters.put(ConfigurationSection.class, new YamlRepresenter.RepresentConfigurationSection());
      this.multiRepresenters.put(ConfigurationSerializable.class, new YamlRepresenter.RepresentConfigurationSerializable());
   }

   private class RepresentConfigurationSerializable extends SafeRepresenter.RepresentMap {
      private RepresentConfigurationSerializable() {
         super();
      }

      public Node representData(Object data) {
         ConfigurationSerializable serializable = (ConfigurationSerializable)data;
         Map<String, Object> values = new LinkedHashMap();
         values.put("==", ConfigurationSerialization.getAlias(serializable.getClass()));
         values.putAll(serializable.serialize());
         return super.representData(values);
      }

      // $FF: synthetic method
      RepresentConfigurationSerializable(Object x1) {
         this();
      }
   }

   private class RepresentConfigurationSection extends SafeRepresenter.RepresentMap {
      private RepresentConfigurationSection() {
         super();
      }

      public Node representData(Object data) {
         return super.representData(((ConfigurationSection)data).getValues(false));
      }

      // $FF: synthetic method
      RepresentConfigurationSection(Object x1) {
         this();
      }
   }
}
