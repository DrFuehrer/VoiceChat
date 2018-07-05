package me.videria.voicechat;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jdk.nashorn.internal.parser.JSONParser;
import net.labymod.api.LabyModAddon;
import net.labymod.api.events.ServerMessageEvent;
import net.labymod.main.LabyMod;
import net.labymod.settings.elements.BooleanElement;
import net.labymod.settings.elements.ControlElement;
import net.labymod.settings.elements.ControlElement.IconData;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.settings.elements.SliderElement;
import net.labymod.utils.Consumer;
import net.labymod.utils.Debug;
import net.labymod.utils.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import scala.util.parsing.json.JSONObject;

public class VoiceChat extends LabyModAddon {

	private static VoiceChat instance;
	
	private int voiceRange;
	private boolean threeDSound = true;
	private boolean active = true;
	private double requestInterval = 0.2;
	private Minecraft mc;
	private HashMap<String, Boolean> servers = new HashMap<String, Boolean>();
	private ArrayList<String> activatedByServer = new ArrayList<String>();
	private HashMap<String, VoiceServer> serverInfo = new HashMap<String, VoiceServer>();
	
	/**
	 * Called when the addon gets enabled
	 */
	@Override
	public void onEnable() {
		instance = this;
		mc = Minecraft.getMinecraft();
		
	    VoiceModul module = new VoiceModul();
		getApi().registerModule(module);
	    getApi().registerForgeListener( this );
	    

		
		getApi().getEventManager().register( new ServerMessageEvent() {
		    public void onServerMessage( String messageKey, JsonElement serverMessage ) {
		         if( messageKey.equals( "ActivateVoiceChat" )) {
		        	if(!activatedByServer.contains(mc.getCurrentServerData().serverIP)) {
		        		activatedByServer.add(mc.getCurrentServerData().serverIP);
		        	}
		        }
		        if( messageKey.equals( "DeactivateVoiceChat" )) {
		        	if(activatedByServer.contains(mc.getCurrentServerData().serverIP)) {
		        		activatedByServer.remove(mc.getCurrentServerData().serverIP);
		        	}
		        }
		    }

		} );
	}

	public static VoiceChat getInstance() {
		return instance;
	}
	
	public Minecraft getMc() {
		return mc;
	}
	
	public boolean getActive() {
		return active;
	}
	
	public HashMap<String, VoiceServer> getServerInfo() {
		return serverInfo;
	}
	
	public ArrayList<String> getActivatedServer() {
		return activatedByServer;
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
		this.voiceRange = getConfig().has("voiceRange") ? getConfig().get("voiceRange").getAsInt() : 10;
		this.threeDSound = getConfig().has("threeDSound") ? getConfig().get("threeDSound").getAsBoolean() : true;
	}

	/**
	 * Called when the addon's ingame settings should be filled
	 *
	 * @param subSettings
	 *            a list containing the addon's settings' elements
	 */
	@Override
	protected void fillSettings(List<SettingsElement> subSettings) {
		subSettings.add(new SliderElement("Voice Range", this, new IconData(Material.ITEM_FRAME), "voiceRange", this.voiceRange).setRange(3, 15));
		subSettings.add(new BooleanElement("3D Sound", this, new IconData(Material.ANVIL), "threeDSound", this.threeDSound));

		subSettings.add( new BooleanElement( "Enabled", new ControlElement.IconData( Material.LEVER ), new Consumer<Boolean>() {
		    @Override
		    public void accept( Boolean accepted ) {
		    	active = accepted;
		    }
		}, true ) );
	}

	private long lastRequest;
	private static final ExecutorService SCHEDULER = Executors.newFixedThreadPool(10);

	@SubscribeEvent
	public void onTick(TickEvent.ClientTickEvent event) {
		if ((LabyMod.getInstance().isInGame()) && (System.currentTimeMillis() > lastRequest) && mc.getCurrentServerData() != null && active) {
			lastRequest = (System.currentTimeMillis() + (int) (requestInterval * 1000L));
			
			SCHEDULER.execute(new Runnable() {
				public void run() {

					
					try {
						if(!servers.containsKey(mc.getCurrentServerData().serverIP)) {
							JsonParser parser = new JsonParser();
							JsonObject info = parser.parse(org.apache.commons.io.IOUtils.toString(new URL("http://videria.cf/Minecraft/VoiceChat/api.php?ip=" + mc.getCurrentServerData().serverIP.toLowerCase().replace(":25565", "") + "&username=" + mc.thePlayer.getName()))).getAsJsonObject();
							
							System.out.println("Server: " + mc.getCurrentServerData().serverIP + " Enabled: " + info.get("enabled").getAsString());
							
							if(info.get("enabled").getAsString().equals("true")) {
								serverInfo.put(mc.getCurrentServerData().serverIP, new VoiceServer(info.get("channelName").getAsString(), info.get("channelPassword").getAsString(), mc.getCurrentServerData().serverIP));
								servers.put(mc.getCurrentServerData().serverIP, true);
							} else {
								servers.put(mc.getCurrentServerData().serverIP, false);
							}
						} else {
							if(servers.get(mc.getCurrentServerData().serverIP) && activatedByServer.contains(mc.getCurrentServerData().serverIP)) {
								String playerInfo = "";
								java.util.List<EntityPlayer> list = Minecraft.getMinecraft().theWorld.playerEntities;
								for(EntityPlayer player : list) {
									if(player.getName() != Minecraft.getMinecraft().thePlayer.getName()) {
										double x1 = mc.thePlayer.posX;
										double y1 = mc.thePlayer.posY;
										double z1 = mc.thePlayer.posZ;
										double x2 = player.posX;
										double y2 = player.posY;
										double z2 = player.posZ;
										double distance = Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) + Math.pow(z1 - z2, 2));
										
										if(distance <= voiceRange) {
											Vec3 vector = player.getPositionVector().subtract(mc.thePlayer.getPositionVector());
											
											double  rotation = mc.thePlayer.cameraYaw / 180;
											double x = vector.xCoord * Math.cos(rotation) - vector.yCoord * Math.sin(rotation);
											double y = vector.xCoord * Math.sin(rotation) + vector.yCoord * Math.cos(rotation);
											x = x * 10 / voiceRange;
											y = y * 10 / voiceRange;
											
											double volume = ((distance / voiceRange) * -50) + 10;
											
											if(threeDSound) {
												if(playerInfo.equals("")) {
													playerInfo = player.getName() + "~" + x + "~" + y + "~0~" + volume;
												} else {
													playerInfo = playerInfo + ";" + player.getName() + "~" + x + "~" + y + "~0~" + volume;
												}
											} else {
												if(playerInfo.equals("")) {
													playerInfo = player.getName() + "~" + 0 + "~" + 0 + "~0~" + volume;
												} else {
													playerInfo = playerInfo + ";" + player.getName() + "~" + 0 + "~" + 0 + "~0~" + volume;
												}
											}
											System.out.println("Player: " + player.getName() + " Distance: " + distance + " 3D: (" + x + "|" + y + ") Volume: " + volume);
										}
									}
								}
								org.apache.commons.io.IOUtils.toString(new URL("http://localhost:15555/custom_players2/" + serverInfo.get(mc.getCurrentServerData().serverIP).getChannelName() + "/" + serverInfo.get(mc.getCurrentServerData().serverIP).getChannelPassword() + "/" + mc.thePlayer.getName() + "/" + playerInfo));

							}
						}
					} catch (Exception e) {
						System.out.println("Error: " + e.getMessage());
					}
				}
			});
		}
	}
	
	public class VoiceServer {
		
		private String channelName;
		private String channelPassword;
		private String ip;
		
		public VoiceServer(String channelName, String channelPassword, String ip) {
			this.channelName = channelName;
			this.channelPassword = channelPassword;
			this.ip = ip;
		}
		
		public String getChannelName() {
			return channelName;
		}
		
		public String getChannelPassword() {
			return channelPassword;
		}
		
		public String getIP() {
			return ip;
		}
		
	}

}
