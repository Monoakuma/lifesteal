package io.github.monoakuma.lifesteal.strykae;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.mutable.PotionEffectData;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.world.World;

import java.util.Random;

import static org.spongepowered.api.world.weather.Weathers.*;

public class strykaeStatusUpdate implements Runnable {
    private Strykae stry = Strykae.getPlugin();
    private Logger logger = stry.getLogger();
    @Override
    public void run() {
        World world = Sponge.getServer().getWorld("world").get();
        Random rand = new Random();
        for (Player p : world.getPlayers()) {
            int L = stry.getPlayerNode(p).getNode("lives").getInt();
            boolean isDay = world.getProperties().getWorldTime()%24000<13000;
            PotionEffectData sEffects = p.getOrCreate(PotionEffectData.class).get();
            if (!stry.isStrykae(p) && L>-3 && stry.getPlayerNode(p).getNode("mana").getInt()<stry.getPlayerNode(p).getNode("manaX").getInt() && isDay) {
                stry.getPlayerNode(p).getNode("mana").setValue(stry.getPlayerNode(p).getNode("mana").getInt() + 1);
                if (stry.getPlayerNode(p).getNode("mana").getInt()%10==0) p.sendTitle(Title.builder().actionBar(Text.of("You have " + stry.getPlayerNode(p).getNode("mana").getInt() + " mana.")).build());
            } else if (stry.isStrykae(p) && !stry.isGhost(p) && L>2 && stry.getPlayerNode(p).getNode("mana").getInt()<stry.getPlayerNode(p).getNode("manaX").getInt() && !isDay) {
                stry.getPlayerNode(p).getNode("mana").setValue(stry.getPlayerNode(p).getNode("mana").getInt() + 1);
                if (stry.getPlayerNode(p).getNode("mana").getInt()%10==0) p.sendTitle(Title.builder().actionBar(Text.of("You have " + stry.getPlayerNode(p).getNode("mana").getInt() + " mana.")).build());
            }
            if (stry.isStrykae(p) && L>=5 && world.getHighestYAt(p.getLocation().getBlockX(),p.getLocation().getBlockZ())<=p.getLocation().getBlockY() && isDay && world.getWeather() == CLEAR) {
                sEffects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.WEAKNESS).duration(200).amplifier(Math.min(L-2,4)).build());
                p.offer(sEffects);
            }
            if (stry.isStrykae(p) && L>=6 && world.getHighestYAt(p.getLocation().getBlockX(),p.getLocation().getBlockZ())<=p.getLocation().getBlockY()&& world.getHighestYAt(p.getLocation().getBlockX(),p.getLocation().getBlockZ())<=p.getLocation().getBlockY() && world.getWeather() == RAIN) {
                sEffects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.INVISIBILITY).duration(200).amplifier(0).build());
                p.offer(sEffects);
            }
            if (stry.isStrykae(p) && L>=9 && world.getHighestYAt(p.getLocation().getBlockX(),p.getLocation().getBlockZ())<=p.getLocation().getBlockY()&& world.getHighestYAt(p.getLocation().getBlockX(),p.getLocation().getBlockZ())<=p.getLocation().getBlockY() && world.getWeather() == THUNDER_STORM) {
                sEffects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.RESISTANCE).duration(200).amplifier(3).build());
                p.offer(sEffects);
            }
            if (stry.isStrykae(p) && L>=5 && !isDay){
                sEffects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.STRENGTH).duration(200).amplifier(Math.min(L-5,1)).build());
                sEffects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.NIGHT_VISION).duration(400).amplifier(0).build());
                p.offer(sEffects);
            }

            if (!stry.isStrykae(p) && !stry.isGhost(p) && stry.getPlayerNode(p).getNode("infected").getBoolean()){
                sEffects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.HUNGER).duration(200).amplifier(0).build());
                sEffects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.WEAKNESS).duration(200).amplifier(1).build());
                if (rand.nextInt(100)<3){
                    sEffects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.POISON).duration(200).amplifier(0).build());
                    p.playSound(SoundTypes.ENTITY_PLAYER_BREATH, p.getPosition(), 1);
                }
                p.offer(sEffects);
                for (Entity e : p.getNearbyEntities(7.0)){
                    if (e.getType()==EntityTypes.PLAYER && rand.nextInt(10)<2){
                        stry.getPlayerNode(e).getNode("infected").setValue(true);
                    }
                }
            }

            //Concentration Spells
            if (stry.getPlayerNode(p).getNode("SpellTimer").getInt()>0 && stry.getPlayerNode(p).getNode("ActiveSpell").getString()=="Saena"){
                stry.getPlayerNode(p).getNode("SpellTimer").setValue(stry.getPlayerNode(p).getNode("SpellTimer").getInt()-1);
                ParticleEffect particle = ParticleEffect.builder().type(ParticleTypes.LARGE_SMOKE).quantity(15).build();
                p.spawnParticles(particle, p.getPosition());
                sEffects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.INVISIBILITY).duration(200).amplifier(0).build());
                p.offer(sEffects);
            } else if (stry.getPlayerNode(p).getNode("SpellTimer").getInt()>0 && stry.getPlayerNode(p).getNode("ActiveSpell").getString()=="Kane"){
                if (!isDay) {
                    stry.getPlayerNode(p).getNode("SpellTimer").setValue(stry.getPlayerNode(p).getNode("SpellTimer").getInt() - 1);
                    for (Player player : world.getPlayers()) {
                        for (Entity e : player.getNearbyEntities(70)) {
                            if (e.getType() == EntityTypes.SKELETON) {
                                e.remove();
                                Entity witherskeleton = world.createEntityNaturally(EntityTypes.WITHER_SKELETON, e.getLocation().getPosition());
                                world.spawnEntity(witherskeleton);
                            }
                        }
                    }
                } else {stry.getPlayerNode(p).getNode("SpellTimer").setValue(0);}
            }
            //Concentration Expirations
            if (stry.getPlayerNode(p).getNode("ActiveSpell").getString()=="Kane" && stry.getPlayerNode(p).getNode("SpellTimer").getInt()<=0){ stry.getPlayerNode(p).getNode("ActiveSpell").setValue(""); }

        }
    }
}
