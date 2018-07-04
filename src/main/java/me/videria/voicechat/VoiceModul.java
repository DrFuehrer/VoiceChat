package me.videria.voicechat;

import net.labymod.ingamegui.moduletypes.SimpleModule;
import net.labymod.settings.elements.ControlElement.IconData;
import net.labymod.utils.Debug;
import net.labymod.utils.Material;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class VoiceModul extends SimpleModule {

	@Override
	public String getDefaultValue() {
		return "Inaktive";
	}

	@Override
	public String getDisplayName() {
		return "VoiceChat";
	}

	@Override
	public String getDisplayValue() {
		if(VoiceChat.getInstance() == null) {
	        return "Inaktive (No Instance)";
		}
		if(VoiceChat.getInstance().getMc().getCurrentServerData() != null) {
			if(VoiceChat.getInstance().getServerInfo().containsKey(VoiceChat.getInstance().getMc().getCurrentServerData().serverIP) && VoiceChat.getInstance().getActive()) {
				return "Aktive";
			} else {
		        return "Inaktive (Server not Supported)";
			}
		} else {
	        return "Inaktive (No MC)";
		}
	}

	@Override
	public String getDescription() {
        return "Display if the Server is supporting VoiceChat addon.";
	}

	@Override
	public IconData getIconData() {
		if(VoiceChat.getInstance() == null) {
	        return new IconData( Material.REDSTONE_TORCH_OFF );
		}
		if(VoiceChat.getInstance().getMc().getCurrentServerData() != null) {
			if(VoiceChat.getInstance().getServerInfo().containsKey(VoiceChat.getInstance().getMc().getCurrentServerData().serverIP) && VoiceChat.getInstance().getActive()) {
		        return new IconData( Material.REDSTONE_TORCH_ON );
			} else {
		        return new IconData( Material.REDSTONE_TORCH_OFF );
			}
		} else {
	        return new IconData( Material.REDSTONE_TORCH_OFF );
		}
	}

	@Override
	public String getSettingName() {
        return "VoiceChat Status";
	}

	@Override
	public int getSortingId() {
		return 0;
	}

	@Override
	public void loadSettings() {
	}

}
