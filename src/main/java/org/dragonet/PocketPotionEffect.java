package org.dragonet;

import java.util.HashMap;

public class PocketPotionEffect {
   public static final int SPEED = 1;
   public static final int SLOWNESS = 2;
   public static final int HASTE = 3;
   public static final int SWIFTNESS = 3;
   public static final int FATIGUE = 4;
   public static final int MINING_FATIGUE = 4;
   public static final int STRENGTH = 5;
   public static final int JUMP = 8;
   public static final int NAUSEA = 9;
   public static final int CONFUSION = 9;
   public static final int REGENERATION = 10;
   public static final int DAMAGE_RESISTANCE = 11;
   public static final int FIRE_RESISTANCE = 12;
   public static final int WATER_BREATHING = 13;
   public static final int INVISIBILITY = 14;
   public static final int WEAKNESS = 18;
   public static final int POISON = 19;
   public static final int WITHER = 20;
   public static final int HEALTH_BOOST = 21;
   private static final HashMap<Integer, PocketPotionEffect> EFFECTS = new HashMap();
   private final int effect;
   private int ampilifier;
   private int duration;
   private boolean particles;

   public static PocketPotionEffect getByID(int id) {
      return EFFECTS.containsKey(id) ? ((PocketPotionEffect)EFFECTS.get(id)).clone() : null;
   }

   public PocketPotionEffect(int effect) {
      this.effect = effect;
   }

   protected PocketPotionEffect clone() {
      PocketPotionEffect eff = new PocketPotionEffect(this.effect);
      eff.setAmpilifier(this.ampilifier);
      eff.setParticles(this.particles);
      eff.setDuration(this.duration);
      return eff;
   }

   public int getEffect() {
      return this.effect;
   }

   public int getAmpilifier() {
      return this.ampilifier;
   }

   public void setAmpilifier(int ampilifier) {
      this.ampilifier = ampilifier;
   }

   public int getDuration() {
      return this.duration;
   }

   public void setDuration(int duration) {
      this.duration = duration;
   }

   public boolean isParticles() {
      return this.particles;
   }

   public void setParticles(boolean particles) {
      this.particles = particles;
   }

   static {
      EFFECTS.put(1, new PocketPotionEffect(1));
      EFFECTS.put(2, new PocketPotionEffect(2));
      EFFECTS.put(3, new PocketPotionEffect(3));
      EFFECTS.put(3, new PocketPotionEffect(3));
      EFFECTS.put(4, new PocketPotionEffect(4));
      EFFECTS.put(4, new PocketPotionEffect(4));
      EFFECTS.put(5, new PocketPotionEffect(5));
      EFFECTS.put(8, new PocketPotionEffect(8));
      EFFECTS.put(9, new PocketPotionEffect(9));
      EFFECTS.put(9, new PocketPotionEffect(9));
      EFFECTS.put(10, new PocketPotionEffect(10));
      EFFECTS.put(11, new PocketPotionEffect(11));
      EFFECTS.put(12, new PocketPotionEffect(12));
      EFFECTS.put(13, new PocketPotionEffect(13));
      EFFECTS.put(14, new PocketPotionEffect(14));
      EFFECTS.put(18, new PocketPotionEffect(18));
      EFFECTS.put(19, new PocketPotionEffect(19));
      EFFECTS.put(20, new PocketPotionEffect(20));
      EFFECTS.put(21, new PocketPotionEffect(21));
   }
}
