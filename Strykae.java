package io.github.monoakuma.lifesteal.strykae;


import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.PotionEffectData;
import org.spongepowered.api.data.manipulator.mutable.entity.HealthData;
import org.spongepowered.api.data.manipulator.mutable.entity.RespawnLocationData;
import org.spongepowered.api.data.manipulator.mutable.entity.TameableData;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.IgniteEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.entity.ai.SetAITargetEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.event.item.inventory.UseItemStackEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.RespawnLocation;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Plugin(
		id = "strykae",
		name = "Strykae",
		description = "Strykae is a system that supports the creation of a life economy through hearts that are dropped on death by players that can be eaten to increase one's number of lives, as well as dehumanization and magic",
		url = "https://github.com/Monoakuma/lifesteal",
		authors = {
				"Mono"
		}
)

public class Strykae {
	@Inject
	@DefaultConfig(sharedRoot = true)
	private Path configFile;
	private ConfigurationLoader<CommentedConfigurationNode> configLoader;
	private CommentedConfigurationNode configNode;
	private HashMap<UUID, HashMap<String, Object>> playerDataTemp = new HashMap<UUID, HashMap<String, Object>>();
	private HashMap<UUID, HashMap<String, Object>> tempData = getSessionData();
	private final String[] messageList = {"Your attempt at magically texting everyone fails", "You can't seem to yell loud enough", "Chat does not seem to work anymore",
			"You attempt to chat, but forget what that means", "Your message is sent...but vanishes mysteriously", "Your attempt at yelling infinitely loud fails ","Telepathy has been disabled."};
	/*private final BlockType[] transmuteTable = {BlockTypes.DIRT,BlockTypes.COBBLESTONE,BlockTypes.GRAVEL,BlockTypes.CLAY,BlockTypes.COAL_ORE,BlockTypes.IRON_ORE,BlockTypes.LOG,BlockTypes.LEAVES};
	private final BlockType[] ignitionTable = {BlockTypes.LOG,BlockTypes.NETHERRACK,BlockTypes.PLANKS};
	private final SoundType[] illusionTable = {SoundTypes.AMBIENT_CAVE,SoundTypes.ENTITY_ENDERMEN_AMBIENT,SoundTypes.ENTITY_GENERIC_EXPLODE,SoundTypes.ENTITY_WOLF_AMBIENT,SoundTypes.ENTITY_PLAYER_BREATH,SoundTypes.ITEM_FIRECHARGE_USE,SoundTypes.ENTITY_ENDERMEN_TELEPORT};
	private final ArrayList<BlockType> ignitions = new ArrayList<>(Arrays.asList(ignitionTable));
	private final ArrayList<BlockType> transmutables = new ArrayList<>(Arrays.asList(transmuteTable));
	*/
	private final ItemType[] sFoods={ItemTypes.CARROT,ItemTypes.POTATO,ItemTypes.BAKED_POTATO,
			ItemTypes.POISONOUS_POTATO,ItemTypes.PUMPKIN_PIE,ItemTypes.APPLE,ItemTypes.COOKIE,
			ItemTypes.BREAD,ItemTypes.MUSHROOM_STEW};
	private final ArrayList<ItemType> VFoods = new ArrayList<>(Arrays.asList(sFoods));
	private final EntityType[] worldmonsters={EntityTypes.ZOMBIE,EntityTypes.SKELETON,EntityTypes.CAVE_SPIDER,EntityTypes.SPIDER,EntityTypes.CREEPER,EntityTypes.HUSK,EntityTypes.STRAY,EntityTypes.WITCH,EntityTypes.WITHER_SKELETON,EntityTypes.ZOMBIE_VILLAGER};
	private final ArrayList<EntityType> monsters = new ArrayList<>(Arrays.asList(worldmonsters));
	private final ArrayList<String> stomped = new ArrayList<>(Arrays.asList("forge", "sponge", "me", "tell","restore","mana","say"));
	private Random rand;
	private Server server;
	private World world;
	private static Strykae plugin;
	private final Text[] spelltext = {Text.of("Acheron dipped his fingers into his own gaping wound and wrote the symbol of Heilung upon a paper. He blessed the name of Alatan as he watched his wounds close up."),
			Text.of("A saddle inscribed with Albtraum, with it I shall strip the flesh from my steed and make him worthy of my new title."),
			Text.of("My king is a megalomaniac, - Because of this he constantly tries to find new magic for me - He has found an ancient spell for me, a golden sword inscribed with Sunnon. Possibly the first spell, Sunnon allows me to - point and ignite."),
			Text.of("Trismegistus plucked a feather from his pouch, upon it was written Aldr Caglio and as he crushed it he felt his body lighten, as he ran to deliver the jewels of Galdur."),
			Text.of("I clutch a gold nugget called Perac Buft and my hands quicken."),
			Text.of("Esau made a paper talisman inscribed with Ocuberaht and used it to reveal the traitor, the disorganized crowd launched themselves upon the traitor."),
			Text.of("The plague ravages my lands, I curse it. However, I now have a cure. By writing the word of Laeka upon rabbit's hide I can  cure the afflicted."),
			Text.of("The hearts of these men are so corrupt already. I shall stand in their market square with my bowl inscribed with Tauti and give them a plague to cure their greed."),
			Text.of("They've killed my hunting dog Kahaud for his meat, so I enchant glowing dust with his name and now I hunt them for their hearts."),
			Text.of("Galdur has shared his blessing of Alatan with me. I wipe fiery blazing powder called Lrel Ka upon my face and I rush in with the strength of three men"),
			Text.of("These humans were fools to follow me into this cave, I point an iron sword inscribed with Menon and freeze their limbs to a crawl. They are now only prey."),
			Text.of("Galdur tries to intrude on my domain, so I write the name of the first murderer, Kane upon a ghast's tear. The night shall grab them with cold hands."),
			Text.of("My king is a megalomaniac, so he - he has unknowingly given - powerful offensive magic designed by man. I on a blaze rod I inscribe the word Caestrum, literally meaning Heavenly Noise"),
			Text.of("Galdur's court wizard was smart, but not wise. His insight has shown me the power of life. I write Isati upon an arrow and sacrifice a life. Dust to Dust Galdur's kingdom, burn with your sins...")};
	private final String[] spellnames = {"Heilung","Albtraum","Sunnon","Aldr Caglio","Perac Buft","Ocuberaht","Laeka","Tauti","Kahaud","Lrel Ka","Menon","Kane","Caestrum","Isati"};
	private final String[] spellauthor = {"Acheron","Nachrin II","Sifan","Trimegistus","Savon",   "Esau",   "Elisa","Morgiana","Rook","Aston", "Rook", "Erik","Sifan",   "Khan"};
	@Inject
	private Logger logger;

	@Listener
	public void onPreInit(GamePreInitializationEvent event) {
		server = Sponge.getServer();
		plugin = this;
		configLoader = HoconConfigurationLoader.builder().setPath(configFile).build();
		rand = new Random();
		setupData();
	}

	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		logger.info("Within an endless sea of mud, Sah opens their eyes...");
		world = server.getWorld("world").get();

		CommandSpec resetpCmd = CommandSpec.builder().description(Text.of("Resets the player's life and humanity")).permission("strykae.pReset").arguments(
				GenericArguments.playerOrSource(Text.of("player"))).executor((CommandSource src, CommandContext args) -> {
			Player player = args.<Player>getOne("player").get();
			CommentedConfigurationNode playerNode = getPlayerNode(player);
			playerNode.getNode("isStrykae").setValue(false);
			playerNode.getNode("lives").setValue(3);
			playerNode.getNode("infected").setValue(false);
			player.setSleepingIgnored(false);
			player.offer(Keys.VANISH,false);
			player.offer(Keys.VANISH_PREVENTS_TARGETING,false);
			player.offer(Keys.VANISH_IGNORES_COLLISION,false);
			src.sendMessage(Text.of("HUMANITY RESTORED"));
			return CommandResult.success();
		}).build();
		CommandSpec manaCmd = CommandSpec.builder().description(Text.of("maxes out player's mana")).permission("strykae.mana").arguments(
				GenericArguments.playerOrSource(Text.of("player"))).executor((CommandSource src, CommandContext args) -> {
			Player player = args.<Player>getOne("player").get();
			CommentedConfigurationNode playerNode = getPlayerNode(player);
			playerNode.getNode("mana").setValue(playerNode.getNode("manaX"));
			src.sendMessage(Text.of("restored mana to max"));
			return CommandResult.success();
		}).build();
		Sponge.getCommandManager().register(this, resetpCmd, "restore");
		Sponge.getCommandManager().register(this,manaCmd,"mana");
		Task.builder().execute(new strykaeStatusUpdate()).interval(4, TimeUnit.SECONDS).submit(this);
	}
	@Listener
	public void playerJoin (ClientConnectionEvent.Join event) {
		Player player = event.getTargetEntity();
		UUID uuid = player.getUniqueId();
		if (!tempData.containsKey(uuid)) {
			tempData.put(uuid, new HashMap<>());
		}
		updatePlayer(player);
		if (isGhost(player)) {
			player.setSleepingIgnored(true);
		}
	}
	@Listener
	public void fuckSeeds(SpawnEntityEvent event) {
		List<Object> blocks = event.getCause().all();
		for (Object block: blocks) {
			if (block instanceof LocatableBlock) {
				block = ((LocatableBlock) block).getBlockState();
			} else if (block instanceof BlockSnapshot) {
				block = ((BlockSnapshot) block).getState();
			}
			if (block instanceof BlockState && ((BlockState) block).getType() == BlockTypes.BEETROOTS) {
				event.setCancelled(true);
				return;
			}
		}
	}

	public boolean isGhost(Entity player) {
		return getPlayerNode(player).getNode("lives").getInt() < 1;
	}

	@Listener
	public void playerIgnite(IgniteEntityEvent event, @First Player player) {
		PotionEffectData Effects = player.getOrCreate(PotionEffectData.class).get();
		if (isStrykae(player) && getPlayerNode(player).getNode("lives").getInt()>=6 && event.getTargetEntity()==player) {
			Effects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.SLOWNESS).duration(600).amplifier(1).build());
			player.offer(Effects);
		}
	}

	@Listener
	public void finishItem(UseItemStackEvent.Finish event, @First Player player) {
		ItemStackSnapshot stack = event.getItemStackInUse();
		PotionEffectData Effects = player.getOrCreate(PotionEffectData.class).get();
		if (stack.getType() == ItemTypes.BEETROOT) {
			//LIFESTEAL
			Optional<List<Text>> lore = stack.get(Keys.ITEM_LORE);
			if (lore.isPresent()) {
				CommentedConfigurationNode playerNode = getPlayerNode(player);
				if (!isStrykae(player)) {
					playerNode.getNode("isStrykae").setValue(true);
					player.sendMessage(Text.builder("You are now a Strykae.").color(TextColors.GOLD).style(TextStyles.ITALIC).build());
					player.playSound(SoundTypes.ENTITY_WOLF_HOWL, player.getPosition(), 2);
					saveData();
				}
				playerNode.getNode("lives").setValue(playerNode.getNode("lives").getInt() + 1); //increase number of lives
				player.playSound(SoundTypes.ENTITY_PLAYER_LEVELUP, player.getPosition(), 2);
				player.sendMessage(Text.builder("You have "+ playerNode.getNode("lives").getInt() +" lives.").color(TextColors.YELLOW).style(TextStyles.ITALIC).build());
			} else event.setCancelled(true);
		} else if (VFoods.contains(stack.getType()) && isStrykae(player)){
			Effects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.HUNGER).duration(600).amplifier(1).build());
			player.offer(Effects);
			player.playSound(SoundTypes.ENTITY_ZOMBIE_HURT , player.getPosition(), 2);
		}
	}
	public boolean isStrykae(Entity p) {
		return getPlayerNode(p).getNode("isStrykae").getBoolean(false);
	}

	/*@Listener
	public void playerInteractBlock (InteractBlockEvent event, @First Player player) {
		BlockSnapshot targetBlock = event.getTargetBlock();
		if (isGhost(player)) {
			CommentedConfigurationNode playerNode = getPlayerNode(player);
			int level = getPlayerNode(player).getNode("level").getInt();
			if (isStrykae(player)) { //WRAITH
				int hrCost = 60/playerNode.getNode("level").getInt();
				if (targetBlock.getState().getType() == BlockTypes.STONE && level>1 && getPlayerNode(player).getNode("mana").getInt() >= hrCost) {
					BlockState state = world.getBlock(targetBlock.getPosition());
					Optional<GroundLuminanceProperty> light = state.getProperty(GroundLuminanceProperty.class);
					if (light.isPresent()){
						if (light.get().getValue()<=0.0){
							Entity undead = world.createEntity(EntityTypes.ZOMBIE,targetBlock.getPosition());
							getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt() - hrCost);
							world.spawnEntity(undead);
						}
					}
				}
			} else if (deathBy(player, DamageTypes.FIRE) || deathBy(player, DamageTypes.MAGMA)) { //DEMON
				if (world.getWeather() == THUNDER_STORM && level>3 && getPlayerNode(player).getNode("mana").getInt() >= 25){
					Entity lightning = world.createEntity(EntityTypes.LIGHTNING,targetBlock.getPosition());
					world.spawnEntity(lightning);
					getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt() - 25);
				}else if (ignitions.contains(targetBlock.getState().getType()) && getPlayerNode(player).getNode("mana").getInt() >= 20) {
					BlockRay<World> blockRay = BlockRay.from(player).whilst(BlockRay.onlyAirFilter()).distanceLimit(2.0).build();
					Optional<BlockRayHit<World>> hitOpt = blockRay.end();
					if (hitOpt.isPresent()) {
						BlockRayHit<World> hit = hitOpt.get();
						world.setBlock(hit.getBlockPosition(), BlockState.builder().blockType(BlockTypes.FIRE).build());
						getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt() - 20);
						player.playSound(SoundTypes.ITEM_FIRECHARGE_USE, player.getPosition(), 1);
					}
				} else if (targetBlock.getState().getType() == BlockTypes.FURNACE || targetBlock.getState().getType() == BlockTypes.LIT_FURNACE && getPlayerNode(player).getNode("mana").getInt() >= 20) {
					BlockState state = targetBlock.getState().getType().getDefaultState();
					FurnaceData fata = Sponge.getDataManager().getManipulatorBuilder(FurnaceData.class).get().create();
					fata.maxBurnTime().set(fata.maxBurnTime().get() + playerNode.getNode("level").getInt() * 8);
					getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt() - 20);
					BlockState newstate = state.with(fata.asImmutable()).get();
					Location<World> blockloc = targetBlock.getLocation().get();
					blockloc.setBlock(newstate);
				} else if (targetBlock.getState().getType() == BlockTypes.GRAVEL && level>2 && getPlayerNode(player).getNode("mana").getInt() >= 30) {
					targetBlock.getLocation().get().setBlock(BlockTypes.LAVA.getDefaultState());
					getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt() - 30);
				}
			} else if (deathBy(player, DamageTypes.MAGIC) || deathBy(player, DamageTypes.VOID)) { //AGATHODAIMON
				if (transmutables.contains(targetBlock.getState().getType()) && getPlayerNode(player).getNode("mana").getInt() >= 20) {
					int i = findIndex(transmuteTable,targetBlock.getState().getType());
					if (i > transmuteTable.length) {
						i=0;
					}
					targetBlock.getLocation().get().setBlock(transmuteTable[i].getDefaultState());
					getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt() - 20);
				} else if (targetBlock.getState().getType()==BlockTypes.IRON_BARS && level>3 && getPlayerNode(player).getNode("mana").getInt() >= 100) {
					player.getWorld().setWeather(Weathers.THUNDER_STORM, 12000); }
			} else if (deathBy(player, DamageTypes.HUNGER) || deathBy(player, DamageTypes.SUFFOCATE)) { //WENDIGO
				if (targetBlock.getState().getType() == BlockTypes.WHEAT || targetBlock.getState().getType() == BlockTypes.CARROTS || targetBlock.getState().getType() == BlockTypes.POTATOES && getPlayerNode(player).getNode("mana").getInt() >= 20) {
					targetBlock.getLocation().get().setBlock(BlockTypes.AIR.getDefaultState());
					getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt() - 20);
				}
			} else if (!isStrykae(player)) { //GEIST
				if (targetBlock.getState().getType() == BlockTypes.IRON_BLOCK && level>2 && getPlayerNode(player).getNode("mana").getInt() >= 40) {
					Entity golem = world.createEntity(EntityTypes.IRON_GOLEM,targetBlock.getPosition());
					golem.offer(Keys.PLAYER_CREATED,true);
					targetBlock.getLocation().get().setBlock(BlockTypes.AIR.getDefaultState());
					world.spawnEntity(golem);
					getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt() - 40);
				} if (targetBlock.getState().getType() == BlockTypes.STONE && level>1 && getPlayerNode(player).getNode("mana").getInt() >= 20) {
					targetBlock.getLocation().get().setBlock(BlockTypes.AIR.getDefaultState());
					getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt() - 20);
				}
			}
			if (targetBlock.getState().getType() == BlockTypes.GOLD_BLOCK && level>4 && getPlayerNode(player).getNode("mana").getInt() >= 100) {
				getPlayerNode(player).getNode("manifest").setValue(true);
				getPlayerNode(player).getNode("mana").setValue(0);
				getPlayerNode(player).getNode("manaX").setValue(0);
			}
		}
	}

	@Listener
	public void playerInteractEntity (InteractEntityEvent event, @First Player player) {
		int level = getPlayerNode(player).getNode("level").getInt();
		Entity target = event.getTargetEntity();
		if (isGhost(player)) {
			if (isStrykae(player)) { //WRAITH
				if (target.getType()==EntityTypes.ZOMBIE) {
					getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt()+1);
					target.remove();
					player.playSound(SoundTypes.ENTITY_ZOMBIE_DEATH, player.getPosition(), 1,0.5);
					ParticleEffect particle = ParticleEffect.builder().type(ParticleTypes.LARGE_SMOKE).quantity(75).build();
					player.spawnParticles(particle, target.getLocation().getPosition());
				}
			} else if (deathBy(player, DamageTypes.FIRE) || deathBy(player, DamageTypes.MAGMA)) { //DEMON
				if (target.getType()==EntityTypes.PLAYER && level>1 && getPlayerNode(player).getNode("mana").getInt() >= 20){
					PotionEffectData Effects = target.getOrCreate(PotionEffectData.class).get();
					Effects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.FIRE_RESISTANCE).duration(1300).amplifier(0).build());
					target.offer(Effects);
					getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt() - 20);
				}
			} else if (deathBy(player, DamageTypes.MAGIC) || deathBy(player, DamageTypes.VOID)) { //AGATHODAIMON
				if (target.getType()==EntityTypes.PLAYER && level>1 && getPlayerNode(player).getNode("mana").getInt() >= 20){
					if (getPlayerNode(target).getNode("mana").getInt() < getPlayerNode(target).getNode("manaX").getInt()) {
						getPlayerNode(target).getNode("mana").setValue(Math.min(getPlayerNode(target).getNode("mana").getInt()+10,getPlayerNode(target).getNode("manaX").getInt()));
						getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt() - 20);
					}

				}
			} else if (deathBy(player, DamageTypes.HUNGER) || deathBy(player, DamageTypes.SUFFOCATE)) { //WENDIGO
				if (target.getType()==EntityTypes.PLAYER && getPlayerNode(player).getNode("mana").getInt() >= 20){
					PotionEffectData Effects = target.getOrCreate(PotionEffectData.class).get();
					Effects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.HUNGER).duration(12*level).amplifier(Math.min(level,4)).build());
					target.offer(Effects);
					player.playSound(SoundTypes.ENTITY_PLAYER_BURP, player.getPosition(), 1);
					getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt() - 20);
				} else if (target.getType()==EntityTypes.COW || target.getType()==EntityTypes.SHEEP && level>3 && getPlayerNode(player).getNode("mana").getInt() >= 20){
					for (Entity e : target.getNearbyEntities(7)) {
						if (e.getType()==EntityTypes.PLAYER) {
							if (!isStrykae(e) && !getPlayerNode(e).getNode("infected").getBoolean()) {
								getPlayerNode(e).getNode("infected").setValue(true);
							}
						}
					}
				} else if (!isStrykae(player)){
					player.playSound(illusionTable[rand.nextInt(6)], player.getPosition(), rand.nextInt(21) / 10F, rand.nextInt(21) / 10F);
				}
			}
		}
	}*/


	@Listener
	public void playerDeath (DestructEntityEvent.Death event, @First Player player) {
		CommentedConfigurationNode playerNode = getPlayerNode(player);
		if (player.get(Keys.HEALTH).get() < 1 && playerNode.getNode("lives").getInt() > 0 && !isGhost(player)) {
			List<Text> sign = Collections.singletonList(Text.builder("It is still alive.").color(TextColors.DARK_RED).style(TextStyles.ITALIC).build());
			ItemStack heart = ItemStack.builder().itemType(ItemTypes.BEETROOT).quantity(1).build();
			heart.offer(Keys.DISPLAY_NAME, Text.builder(player.getName() + "'s Heart").color(TextColors.RED).build());
			heart.offer(Keys.ITEM_LORE, sign);
			Entity item = world.createEntity(EntityTypes.ITEM, player.getLocation().getPosition());
			item.offer(Keys.REPRESENTED_ITEM, heart.createSnapshot());
			world.spawnEntity(item);
			playerNode.getNode("lives").setValue(playerNode.getNode("lives").getInt() - 1); //Lose Lives
			playerNode.getNode("SpellTimer").setValue(0);
			player.sendMessage(Text.builder("You have " + (playerNode.getNode("lives").getInt()) + " lives.").color(TextColors.YELLOW).style(TextStyles.ITALIC).build());
			updatePlayer(player);
			saveData();
			player.playSound(SoundTypes.ENTITY_ZOMBIE_VILLAGER_CURE, player.getPosition(), 2);

			Optional<RespawnLocation> loc = player.get(RespawnLocationData.class).flatMap(respawnLocationData -> respawnLocationData.getForWorld(world));
			if (loc.isPresent()) {
				player.setLocation(loc.get().getPosition(), world.getUniqueId());
			} else {
				player.setLocation(world.getSpawnLocation().getPosition(), world.getUniqueId());
			}
			event.setCancelled(true);
			HealthData maxHealth = player.getHealthData().set(Keys.HEALTH, player.get(Keys.MAX_HEALTH).get());
			player.offer(maxHealth);
		}
	}

	@Listener
	public void itemInteract(InteractItemEvent event, @First Player player) {
		ItemStackSnapshot stack = event.getItemStack();
		Optional<Text> name = stack.get(Keys.DISPLAY_NAME);
		boolean isDay = world.getProperties().getWorldTime()%24000<13000;
		if (name.isPresent()) {
			Text itemname = name.get().toText();
			//GALDUR SPELLS
			if (stack.getType() == ItemTypes.PAPER && itemname.equals(Text.of("Ocuberaht")) && getPlayerNode(player).getNode("mana").getInt()>23 && !isStrykae(player)) {
				getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt()-24);
				for (Entity e : player.getNearbyEntities(20)) {
					if (e.getType() == EntityTypes.PLAYER) {
						if (isStrykae(e)) {
							PotionEffectData Effects = e.getOrCreate(PotionEffectData.class).get();
							Effects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.GLOWING).duration((getPlayerNode(e).getNode("lives").getInt()-3) * 60).amplifier(1).build());
							e.offer(Effects);
						}
					}
				}
			}
			else if (stack.getType() == ItemTypes.FEATHER && itemname.equals(Text.of("Aldr Caglio")) && getPlayerNode(player).getNode("mana").getInt()>6 && !isStrykae(player)) {
				PotionEffectData Effects = player.getOrCreate(PotionEffectData.class).get();
				Effects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.SPEED).duration(200).amplifier(2).build());
				player.offer(Effects);
				getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt()-7);
			}
			else if (stack.getType() == ItemTypes.BLAZE_POWDER && itemname.equals(Text.of("Lrel Ka")) && getPlayerNode(player).getNode("mana").getInt()>11 && !isStrykae(player)) {
				PotionEffectData Effects = player.getOrCreate(PotionEffectData.class).get();
				Effects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.STRENGTH).duration(200).amplifier(4-getPlayerNode(player).getNode("lives").getInt()).build());
				player.offer(Effects);
				getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt()-12);
			}
			else if (stack.getType() == ItemTypes.PAPER && itemname.equals(Text.of("Heilung")) && getPlayerNode(player).getNode("mana").getInt()>23 && !isStrykae(player)) {
				PotionEffectData Effects = player.getOrCreate(PotionEffectData.class).get();
				Effects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.REGENERATION).duration(200).amplifier(2).build());
				player.offer(Effects);
				getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt()-24);
			}
			else if (stack.getType() == ItemTypes.GOLD_NUGGET && itemname.equals(Text.of("Perac Buft")) && getPlayerNode(player).getNode("mana").getInt()>6 && !isStrykae(player)) {
				PotionEffectData Effects = player.getOrCreate(PotionEffectData.class).get();
				Effects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.HASTE).duration(200).amplifier(2).build());
				player.offer(Effects);
				getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt()-7);
			}
			else if (stack.getType() == ItemTypes.GOLDEN_SWORD && itemname.equals(Text.of("Sunnon")) && getPlayerNode(player).getNode("mana").getInt()>15 && !isStrykae(player)) {
				BlockRay<World> blockRay = BlockRay.from(player).whilst(BlockRay.onlyAirFilter()).distanceLimit(80.0).build();
				Optional<BlockRayHit<World>> hitOpt = blockRay.end();
				if (hitOpt.isPresent()) {
					BlockRayHit<World> hit = hitOpt.get();
					world.setBlock(hit.getBlockPosition(), BlockState.builder().blockType(BlockTypes.FIRE).build());
					getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt()-16);
					player.playSound(SoundTypes.ITEM_FIRECHARGE_USE , player.getPosition(), 1);
				}
			}
			else if (stack.getType() == ItemTypes.RABBIT_HIDE && itemname.equals(Text.of("Laeka")) && getPlayerNode(player).getNode("mana").getInt()>=40 && !isStrykae(player)) {
				if (rand.nextInt(10)<2) {
					for (Entity e : player.getNearbyEntities(7.0)) {
						player.playSound(SoundTypes.ENTITY_ZOMBIE_VILLAGER_CURE, player.getPosition(), 2);
						getPlayerNode(e).getNode("infected").setValue(false);
					}
				} else {
					player.playSound(SoundTypes.ENTITY_ELDER_GUARDIAN_CURSE, player.getPosition(), 1);
				}
				getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt() - 40);

			}
			else if (stack.getType() == ItemTypes.BLAZE_ROD && itemname.equals(Text.of("Caestrum")) && getPlayerNode(player).getNode("mana").getInt()>=80 && !isStrykae(player)) {
				BlockRay<World> blockRay = BlockRay.from(player).whilst(BlockRay.onlyAirFilter()).distanceLimit(80.0).build();
				Optional<BlockRayHit<World>> hitOpt = blockRay.end();
				if (hitOpt.isPresent()) {
					BlockRayHit<World> hit = hitOpt.get();
					Entity lightning = world.createEntity(EntityTypes.LIGHTNING,hit.getPosition());
					Entity explosive = world.createEntity(EntityTypes.ENDER_CRYSTAL,hit.getPosition());
					Explosion.builder().radius(4 * 4 - getPlayerNode(player).getNode("lives").getInt()).location(hit.getLocation()).canCauseFire(true).knockback(8.0).build();
					world.spawnEntity(explosive);
					world.spawnEntity(lightning);
					getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt()-80);
					player.playSound(SoundTypes.ENTITY_GENERIC_EXPLODE , hit.getPosition(), 3);
				}

			}
			//SAH SPELLS
			else if (stack.getType() == ItemTypes.GLOWSTONE_DUST && itemname.equals(Text.of("Kahaud")) && isStrykae(player)){
				for (Entity e : player.getNearbyEntities(30)) {
					if (e.getType() == EntityTypes.PLAYER) {
						if (!isStrykae(e)) {
							PotionEffectData Effects = e.getOrCreate(PotionEffectData.class).get();
							Effects.addElement(PotionEffect.builder().potionType(PotionEffectTypes.GLOWING).duration(Math.min(getPlayerNode(player).getNode("lives").getInt(),4)*getMoonphase()*60).amplifier(1).build());
							e.offer(Effects);
						}
					}
				}
			}
			else if (stack.getType() == ItemTypes.SADDLE && itemname.equals(Text.of("Albtraum")) && isStrykae(player) && !isDay && getPlayerNode(player).getNode("mana").getInt()>=20) {
				for (Entity e : player.getNearbyEntities(2.5)){
					if (e.getType()==EntityTypes.HORSE){
						Entity nightmare = world.createEntity(EntityTypes.SKELETON_HORSE, e.getLocation().getPosition());
						Optional<TameableData> owner = nightmare.get(TameableData.class);
						if (owner.isPresent()) {
							Optional<UUID> summoner = Optional.of(player.getUniqueId());
							nightmare.offer(Keys.TAMED_OWNER, summoner);
						}
						nightmare.offer(Keys.DISPLAY_NAME,Text.of("Albtraum"));
						e.remove();
						world.spawnEntity(nightmare);
					}
				}
				getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt()-20);
			}
			else if (stack.getType() == ItemTypes.IRON_SWORD && itemname.equals(Text.of("Menon")) && isStrykae(player) && getPlayerNode(player).getNode("mana").getInt()>=16) {
				BlockRay<World> blockRay = BlockRay.from(player).whilst(BlockRay.onlyAirFilter()).distanceLimit(20.0).build();
				Optional<BlockRayHit<World>> hitOpt = blockRay.end();
				if (hitOpt.isPresent()) {
					BlockRayHit<World> hit = hitOpt.get();
					Entity fog = world.createEntity(EntityTypes.AREA_EFFECT_CLOUD,hit.getPosition());
					fog.offer(Keys.AREA_EFFECT_CLOUD_COLOR, Color.WHITE);
					fog.offer(Keys.AREA_EFFECT_CLOUD_RADIUS,4.4);
					fog.offer(Keys.AREA_EFFECT_CLOUD_DURATION,12*getMoonphase());
					fog.offer(Keys.AREA_EFFECT_CLOUD_PARTICLE_TYPE, ParticleTypes.CLOUD);
					world.spawnEntity(fog);
					for (Entity e : fog.getNearbyEntities(4.4)) {
						if (isStrykae(player)) {
							Optional<PotionEffectData> Effects = e.getOrCreate(PotionEffectData.class);
							if (Effects.isPresent()) {
								Effects.get().addElement(PotionEffect.builder().potionType(PotionEffectTypes.SLOWNESS).duration(12 * (1+getMoonphase())).amplifier(2).build());
								Effects.get().addElement(PotionEffect.builder().potionType(PotionEffectTypes.MINING_FATIGUE).duration(12 * (1+getMoonphase())).amplifier(2).build());
								e.offer(Effects.get());
							}
						}
					}
					getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt()-16);
					player.playSound(SoundTypes.ENTITY_SNOWMAN_DEATH , player.getPosition(), 1);
				}
			}
			else if (stack.getType() == ItemTypes.GHAST_TEAR && itemname.equals(Text.of("Kane")) && isStrykae(player) && getPlayerNode(player).getNode("SpellTimer").getInt()<1 && !isDay && getPlayerNode(player).getNode("mana").getInt()>=30){
				//Perfect and seamless
				getPlayerNode(player).getNode("SpellTimer").setValue(getMoonphase()*5);
				getPlayerNode(player).getNode("ActiveSpell").setValue("Kane");
				getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt()-30);
			}
			else if (stack.getType() == ItemTypes.BOWL && itemname.equals(Text.of("Tauti")) && isStrykae(player) && getPlayerNode(player).getNode("mana").getInt()>=20) {
				for (Entity e : player.getNearbyEntities(18)) {
					if (e.getType()==EntityTypes.PLAYER) {
						getPlayerNode(e).getNode("infected").setValue(true);
					}
					getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt()-20);
					player.playSound(SoundTypes.ENTITY_ZOMBIE_INFECT , e.getLocation().getPosition(), 5);
				}
			}
			else if (stack.getType() == ItemTypes.ARROW && itemname.equals(Text.of("Isati")) && isStrykae(player) && !isDay && getPlayerNode(player).getNode("mana").getInt()>=40){
				for (Entity e : player.getNearbyEntities(40)) {
					Entity lightning = world.createEntity(EntityTypes.LIGHTNING, e.getLocation().getPosition());
					Entity explosive = world.createEntity(EntityTypes.ENDER_CRYSTAL,e.getLocation().getPosition());
					Explosion.builder().radius(2 * (getPlayerNode(player).getNode("lives").getInt()-1)).location(e.getLocation()).canCauseFire(true).knockback(11.0).build();
					world.spawnEntity(explosive);
					world.spawnEntity(lightning);
					player.playSound(SoundTypes.ENTITY_ENDERMEN_SCREAM , e.getLocation().getPosition(), 10);
				}
				getPlayerNode(player).getNode("lives").setValue(getPlayerNode(player).getNode("lives").getInt()-1);
				player.respawnPlayer();
				player.sendMessage(Text.builder("You have "+ getPlayerNode(player).getNode("lives").getInt() +" lives.").color(TextColors.YELLOW).style(TextStyles.ITALIC).build());
				getPlayerNode(player).getNode("mana").setValue(getPlayerNode(player).getNode("mana").getInt()-40);
			}
		}
	}

	@Listener
	public void playerTarget (SetAITargetEvent event) {
		Entity targeter = event.getTargetEntity();
		if (event.getTarget().isPresent()) {
			if (event.getTarget().get() instanceof Player) {
				if (isStrykae(event.getTarget().get()) && getPlayerNode(event.getTarget().get()).getNode("lives").getInt()>=6 && monsters.contains(targeter.getType())) {
					event.setCancelled(true);
				}
			}
		}
	}

	@Listener
	public void playerCommand (SendCommandEvent event, @First Player player) {
		Optional<? extends CommandMapping> commandMap = Sponge.getCommandManager().get(event.getCommand());
		if (commandMap.isPresent()) {
			String command = commandMap.get().getPrimaryAlias();
			if (stomped.contains(command)) {
				player.sendMessage(Text.builder("Your command has been STOMPED by COMMAND STOMPER v2000 Appw Trademark*").color(TextColors.RED).style(TextStyles.ITALIC).build());
				event.setCancelled(true);
				event.setResult(CommandResult.empty());
			}
		}
	}

	@Listener
	public void playerChat (MessageChannelEvent.Chat event) { event.setCancelled(true); }

	@Listener
	public void blockBreak (ChangeBlockEvent.Break event) {
		for (Transaction<BlockSnapshot> bs : event.getTransactions()) {
			if (bs.getOriginal().getState().getType()==BlockTypes.MOB_SPAWNER) {
				int ln=0;
				int r = rand.nextInt(100);
				if (r<=5){ln=0;}
				else if (r<=10) {ln=1;}
				else if (r<=15) {ln=2;}
				else if (r<=25) {ln=3;}
				else if (r<=30) {ln=4;}
				else if (r<=35) {ln=5;}
				else if (r<=40) {ln=6;}
				else if (r<=58) {ln=7;}
				else if (r<=66) {ln=8;}
				else if (r<=70) {ln=9;}
				else if (r<=75) {ln=10;}
				else if (r<=85) {ln=11;}
				else if (r<=97) {ln=12;}
				else if (r<=100) {ln=13;}
				ItemStack lootbook = ItemStack.builder().itemType(ItemTypes.WRITTEN_BOOK).quantity(1).build();
				List<Text> spell = new ArrayList<>(Arrays.asList(spelltext[ln]));
				lootbook.offer(Keys.BOOK_AUTHOR,Text.of(spellauthor[ln]));
				lootbook.offer(Keys.DISPLAY_NAME, Text.builder(spellnames[ln]).build());
				lootbook.offer(Keys.BOOK_PAGES,spell);
				Entity book = world.createEntity(EntityTypes.ITEM,bs.getDefault().getPosition());
				book.offer(Keys.REPRESENTED_ITEM,lootbook.createSnapshot());
				//WORKS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				world.spawnEntity(book);
			}
		}
	}


	/*private void ghostInteract (Cancellable event, Player player, int i, Vector3d v, boolean check_vis) {
		for (Entity e : player.getNearbyEntities(10)) {
			if (e instanceof Player && !isGhost(e) && (!check_vis || canSee((Player) e, v))) {
				event.setCancelled(true);
				break; } } }

	private void ghostInteract (Cancellable event, Player player, int i, Vector3d v) {
		ghostInteract(event, player, i, v, true);
	}
	@Listener
	public void playerDrop (ClickInventoryEvent.Drop event, @First Player player) {
		if (isGhost(player)) {
			if (!event.getTransactions().isEmpty()) {
				ghostInteract(event, player, -1, player.getPosition());
			}
		}
	}

	@Listener
	public void playerDrop2 (DropItemEvent.Pre event, @First Player player) {
		if (isGhost(player)) {
			ghostInteract(event, player, -1, player.getPosition());
		}
	}

	@Listener
	public void playerShoot (UseItemStackEvent.Start event, @First Player player) {
		if (cannot_use.contains(event.getItemStackInUse().getType()) && isGhost(player)) {
			event.setCancelled(true);
		}
	}

	@Listener
	public void playerPickup (ChangeInventoryEvent.Pickup.Pre event, @First Player player) {
		if (isGhost(player)) {
			if (player.getInventory().size() < 3) {
				int i = (int) tempData.get(player.getUniqueId()).get("itemTouchedTicks");
				tempData.get(player.getUniqueId()).put("itemTouchedTicks", i + 1);
				if (i > 40) {
					ghostInteract(event, player, -1, event.getTargetEntity().getLocation().getPosition());
				} else {
					event.setCancelled(true);
				}
			} else {
				event.setCancelled(true);
			}
		}
	}
	public boolean deathBy(Player p, DamageType d) {
		DamageType dType;
		try {
			dType = getPlayerNode(p).getNode("deathType").getValue(TypeToken.of(DamageType.class));
		} catch (ObjectMappingException e) {
			logger.error("Invalid Data in Strykae Config!");
			return false;
		}
		return dType.equals(d); }
	*/

	@Listener
	public void onServerStop (GameStoppingEvent event) { saveData(); }
	private void setupData(){
		try {
            logger.info("loading config!");
			if (!Files.exists(configFile)) {
				Files.createFile(configFile);
			}
			configNode = configLoader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveData() {
		try {
			configLoader.save(configNode);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static int findIndex(BlockType arr[], BlockType t)
	{
		int index = Arrays.binarySearch(arr, t);
		return (index < 0) ? -1 : index;
	}



	public void updatePlayer(Player player) {
		CommentedConfigurationNode playerNode = getPlayerNode(player);
		if (playerNode.getNode("lives").getValue()==null) {
			playerNode.getNode("lives").setValue(3);
			playerNode.getNode("isStrykae").setValue(false);
			playerNode.getNode("infected").setValue(false);
			player.offer(Keys.VANISH,false);
			player.offer(Keys.VANISH_PREVENTS_TARGETING,false);
			player.offer(Keys.VANISH_IGNORES_COLLISION,false);
			playerNode.getNode("mana").setValue(12);
			playerNode.getNode("manaX").setValue(12);
			player.setSleepingIgnored(false);
		}
		if (playerNode.getNode("lives").getInt()<1){
			player.sendMessage(Text.builder("Welcome to the afterlife!").color(TextColors.DARK_AQUA).style(TextStyles.ITALIC).build());
			player.offer(Keys.GAME_MODE, GameModes.SPECTATOR);
			player.setSleepingIgnored(true);
		}
		if (playerNode.getNode("lives").getInt()==3 && !isStrykae(player)) {
			playerNode.getNode("mana").setValue(12);
			playerNode.getNode("manaX").setValue(12);
		} else if (playerNode.getNode("lives").getInt()==2 && !isStrykae(player)) {
			playerNode.getNode("mana").setValue(24);
			playerNode.getNode("manaX").setValue(24);
		} else if (playerNode.getNode("lives").getInt()==1 && !isStrykae(player)) {
			playerNode.getNode("mana").setValue(80);
			playerNode.getNode("manaX").setValue(80);
		}
		if (playerNode.getNode("lives").getInt()<5 && isStrykae(player)) {
			playerNode.getNode("mana").setValue(0);
			playerNode.getNode("manaX").setValue(1);
		} else if (playerNode.getNode("lives").getInt()>=5 && isStrykae(player)) {
			playerNode.getNode("mana").setValue(40);
			playerNode.getNode("manaX").setValue(40);
		}
		saveData();
	}
	public CommentedConfigurationNode getPlayerNode(Entity e) {
		return configNode.getNode(e.getUniqueId().toString());
	}
	public long getDays() {
		return(world.getProperties().getWorldTime()/24000);
	}
	public int getMoonphase() {
		int[] moonphases = {20,15,10,5,0,5,10,15};
		return moonphases[(int) (getDays()%8)];
	}

	public HashMap<UUID, HashMap<String, Object>> getSessionData() { return playerDataTemp; }

	public Logger getLogger() {
		return logger;
	}
	public static Strykae getPlugin() {
		return plugin;
	}
}
