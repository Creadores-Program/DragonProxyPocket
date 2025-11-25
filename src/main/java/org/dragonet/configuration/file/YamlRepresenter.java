package org.dragonet.configuration.file;

import java.util.LinkedHashMap;
import java.util.Map;
import org.dragonet.configuration.ConfigurationSection;
import org.dragonet.configuration.serialization.ConfigurationSerializable;
import org.dragonet.configuration.serialization.ConfigurationSerialization;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.representer.Represent;

public class YamlRepresenter extends Representer {

    public YamlRepresenter() {
        
        this.multiRepresenters.put(ConfigurationSection.class, new RepresentConfigurationSection());
        this.multiRepresenters.put(ConfigurationSerializable.class, new RepresentConfigurationSerializable());
    }

    private class RepresentConfigurationSerializable implements Represent {
        
        public Node representData(Object data) {
            ConfigurationSerializable serializable = (ConfigurationSerializable) data;
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("==", ConfigurationSerialization.getAlias(serializable.getClass()));
            values.putAll(serializable.serialize());
            return YamlRepresenter.this.representData(values);
        }
    }

    private class RepresentConfigurationSection implements Represent {
        
        public Node representData(Object data) {
            Map<String, Object> values = ((ConfigurationSection) data).getValues(false);
            
            return YamlRepresenter.this.representData(values);
        }
    }
}