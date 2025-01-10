package yesman.epicfight.client.gui;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

@OnlyIn(Dist.CLIENT)
public class VersionNotifier {
	private final Minecraft minecraft;
	private int count = 0;
	
	public VersionNotifier(Minecraft minecraft) {
		this.minecraft = minecraft;
	}
	
	public void reset() {
		this.count = 0;
	}
	
	public void render(GuiGraphics guiGraphics, boolean inWorld) {
		if (this.count > 600 && inWorld) {
			return;
		}
		
		this.count++;
		Window sr = this.minecraft.getWindow();
		int width = sr.getGuiScaledWidth();
		
		if (inWorld) {
			String l1 = Component.translatable("epicfight.messages.test_version_warning_line1").getString();
			String l2 = Component.translatable("epicfight.messages.test_version_warning_line2", ModList.get().getModFileById("epicfight").versionString()).getString();
			
			guiGraphics.drawString(this.minecraft.font, l1, (width - this.minecraft.font.width(l1) - 2), 8, 16777215);
			guiGraphics.drawString(this.minecraft.font, l2, (width - this.minecraft.font.width(l2) - 2), 20, 16777215);
		} else {
			String l1 = Component.translatable("epicfight.messages.version_notifier", ModList.get().getModFileById("epicfight").versionString()).getString();
			guiGraphics.drawString(this.minecraft.font, l1, (width - this.minecraft.font.width(l1) - 2), 8, 16777215);
		}
	}
}
