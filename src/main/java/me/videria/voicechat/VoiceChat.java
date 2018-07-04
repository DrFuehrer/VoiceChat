package me.videria.voicechat;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.labymod.api.LabyModAddon;
import net.labymod.main.LabyMod;
import net.labymod.settings.elements.ControlElement.IconData;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.settings.elements.SliderElement;
import net.labymod.utils.Debug;
import net.labymod.utils.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class VoiceChat extends LabyModAddon {

	private int voiceRange;
	private double requestInterval = 0.2;
	private Minecraft mc;
	
	/**
	 * Called when the addon gets enabled
	 */
	@Override
	public void onEnable() {
		mc = Minecraft.getMinecraft();
	    VoiceModul module = new VoiceModul();
	    
	    getApi().registerForgeListener(module);
	    getApi().registerModule(module);
	    getApi().registerForgeListener( this );
	}

	/**
	 * Called when the addon gets disabled
	 */
	@Override
	public void onDisable() {

	}

	/**
	 * Called when this addon's config was loaded and is ready to use
	 */
	@Override
	public void loadConfig() {
		this.voiceRange = getConfig().has("voiceRange") ? getConfig().get("voiceRange").getAsInt() : 3; // <- default
																										// value '5'
	}

	/**
	 * Called when the addon's ingame settings should be filled
	 *
	 * @param subSettings
	 *            a list containing the addon's settings' elements
	 */
	@Override
	protected void fillSettings(List<SettingsElement> subSettings) {
		subSettings
				.add(new SliderElement("Voice Range", this, new IconData(Material.ITEM_FRAME), "voiceRange", this.voiceRange)
						.setRange(3, 15));
	}

	private long lastRequest;
	private static final ExecutorService SCHEDULER = Executors.newFixedThreadPool(10);

	@SubscribeEvent
	public void onTick(TickEvent.ClientTickEvent event) {
		if ((LabyMod.getInstance().isInGame()) && (System.currentTimeMillis() > lastRequest)) {
			lastRequest = (System.currentTimeMillis() + (int) (requestInterval * 1000L));

				SCHEDULER.execute(new Runnable() {
					public void run() {
						try {
							String playerInfo = "";
							java.util.List<EntityPlayer> list = Minecraft.getMinecraft().theWorld.playerEntities;
							for(EntityPlayer player : list) {
								if(player.getName() != Minecraft.getMinecraft().thePlayer.getName()) {
									Debug.log(Debug.EnumDebugMode.ADDON, "Found: " + player.getName());
									double x1 = mc.thePlayer.posX;
									double y1 = mc.thePlayer.posY;
									double z1 = mc.thePlayer.posZ;
									double x2 = player.posX;
									double y2 = player.posY;
									double z2 = player.posZ;
									double distance = Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) + Math.pow(z1 - z2, 2));
									
									if(distance <= voiceRange) {
										Vec3 vector = player.getPositionVector().subtract(mc.thePlayer.getPositionVector());
										
										double  rotation = Math.PI / 180 * (mc.thePlayer.cameraYaw * -1);
										double x = vector.xCoord * Math.cos(rotation) - vector.yCoord * Math.sin(rotation);
										double y = vector.xCoord * Math.sin(rotation) + vector.yCoord * Math.cos(rotation);
										x = x * 10 / voiceRange;
										y = y * 10 / voiceRange;
										
										double volume = (distance < voiceRange ? ((-1/Math.pow((distance - voiceRange), 2))) : 0);
										
										if(playerInfo.equals("")) {
											playerInfo = player.getName() + "~" + 0 + "~" + 0 + "~0~" + volume;
										} else {
											playerInfo = ";" + player.getName() + "~" + 0 + "~" + 0 + "~0~" + volume;
										}
										Debug.log(Debug.EnumDebugMode.ADDON, "Player: " + player.getName() + " Distance: " + distance + " 3D: (" + x + "|" + y + ") Volume: " + volume);
									}
								}
							}
							
							org.apache.commons.io.IOUtils.toString(new URL("http://localhost:15555/custom_players2/[spacer] Minecraft - Sprachchat/43214/" + mc.thePlayer.getName() + "/" + playerInfo));

						} catch (Exception e) {
							Debug.log(Debug.EnumDebugMode.ADDON, "Error: " + e.getMessage());
						}
					}
				});
		}
	}

}
