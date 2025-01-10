package yesman.epicfight.client.gui.screen.config;

import java.io.File;
import java.io.IOException;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.transformer.CustomModelBakery;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.gui.widgets.ColorWidget;
import yesman.epicfight.client.gui.widgets.EpicFightOptionList;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.config.EpicFightOptions;
import yesman.epicfight.config.OptionHandler;
import yesman.epicfight.config.OptionHandler.BooleanOptionHandler;
import yesman.epicfight.config.OptionHandler.IntegerOptionHandler;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class EpicFightGraphicOptionScreen extends EpicFightOptionSubScreen {
	private EpicFightOptionList optionsList;
	
	public EpicFightGraphicOptionScreen(Screen parentScreen, EpicFightOptions config) {
		super(parentScreen, config, Component.translatable("gui." + EpicFightMod.MODID + ".graphic_options"));
	}
	
	@Override
	protected void init() {
		super.init();
		
		this.optionsList = new EpicFightOptionList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
		
		OptionHandler<ClientConfig.HealthBarShowOptions> showHealthIndicator = this.config.healthBarShowOption;
		BooleanOptionHandler showTargetIndicator = this.config.showTargetIndicator;
		BooleanOptionHandler filterAnimation = this.config.filterAnimation;
		BooleanOptionHandler enableAimHelper = this.config.enableAimHelperPointer;
		OptionHandler<Double> aimHelperColor = this.config.aimHelperColor;
		BooleanOptionHandler bloodEffects = this.config.bloodEffects;
		BooleanOptionHandler aimingCorrection = this.config.aimingCorrection;
		BooleanOptionHandler showEpicFightAttributes = this.config.showEpicFightAttributes;
		IntegerOptionHandler maxHitProjectiles = this.config.maxStuckProjectiles;
		BooleanOptionHandler useAnimationShader = this.config.useAnimationShader;
		BooleanOptionHandler firstPersonModel = this.config.firstPersonModel;
		
		int buttonHeight = -32;
		
		Button filterAnimationButton = Button.builder(Component.translatable("gui."+EpicFightMod.MODID+".filter_animation." + (filterAnimation.getValue() ? "on" : "off")), (button) -> {
			filterAnimation.setValue(!filterAnimation.getValue());
			button.setMessage(Component.translatable("gui."+EpicFightMod.MODID+".filter_animation." + (filterAnimation.getValue() ? "on" : "off")));
		}).pos(this.width / 2 + 5, this.height / 4 + buttonHeight).size(160, 20).tooltip(Tooltip.create(title)).build();
		
		Button healthBarShowOptionButton = Button.builder(Component.translatable("gui."+EpicFightMod.MODID+".health_bar_show_option." + showHealthIndicator.getValue().toString()), (button) -> {
			showHealthIndicator.setValue(showHealthIndicator.getValue().nextOption());
			button.setMessage(Component.translatable("gui."+EpicFightMod.MODID+".health_bar_show_option." + showHealthIndicator.getValue().toString()));
		}).pos(this.width / 2 - 165, this.height / 4 - 8).size(160, 20).tooltip(Tooltip.create(Component.translatable("gui."+EpicFightMod.MODID+".filter_animation.tooltip"))).build();
		
		this.optionsList.addSmall(filterAnimationButton, healthBarShowOptionButton);
		
		buttonHeight += 24;
		
		Button showTargetIndicatorButton = Button.builder(Component.translatable("gui."+EpicFightMod.MODID+".target_indicator." + (showTargetIndicator.getValue() ? "on" : "off")), (button) -> {
			showTargetIndicator.setValue(!showTargetIndicator.getValue());
			button.setMessage(Component.translatable("gui."+EpicFightMod.MODID+".target_indicator." + (showTargetIndicator.getValue() ? "on" : "off")));
		}).pos(this.width / 2 + 5, this.height / 4 - 8).size(160, 20).tooltip(Tooltip.create(Component.translatable("gui."+EpicFightMod.MODID+".target_indicator.tooltip"))).build();
		
		Button enableAimHelperButton = Button.builder(Component.translatable("gui."+EpicFightMod.MODID+".aim_helper." + (enableAimHelper.getValue() ? "on" : "off")), (button) -> {
			enableAimHelper.setValue(!enableAimHelper.getValue());
			button.setMessage(Component.translatable("gui."+EpicFightMod.MODID+".aim_helper." + (enableAimHelper.getValue() ? "on" : "off")));
		}).pos(this.width / 2 - 165, this.height / 4 - 8).size(160, 20).tooltip(Tooltip.create(Component.translatable("gui."+EpicFightMod.MODID+".aim_helper.tooltip"))).build();
		
		this.optionsList.addSmall(showTargetIndicatorButton, enableAimHelperButton);
		
		buttonHeight+=24;
		
		Button bloodEffectsButton = Button.builder(Component.translatable("gui."+EpicFightMod.MODID+".blood_effects." + (bloodEffects.getValue() ? "on" : "off")), (button) -> {
			bloodEffects.setValue(!bloodEffects.getValue());
			button.setMessage(Component.translatable("gui."+EpicFightMod.MODID+".blood_effects." + (bloodEffects.getValue() ? "on" : "off")));
		}).pos(this.width / 2 - 165, this.height / 4 - 8).size(160, 20).tooltip(Tooltip.create(Component.translatable("gui."+EpicFightMod.MODID+".blood_effects.tooltip"))).build();
		
		Button exportCustomArmors = Button.builder(Component.translatable("gui."+EpicFightMod.MODID+".export_custom_armor"), (button) -> {
			File resourcePackDirectory = Minecraft.getInstance().getResourcePackDirectory().toFile();
			try {
				CustomModelBakery.exportModels(resourcePackDirectory);
				Util.getPlatform().openFile(resourcePackDirectory);
			} catch (IOException e) {
				EpicFightMod.LOGGER.info("Failed to export custom armor models");
				e.printStackTrace();
			}
		}).pos(this.width / 2 + 5, this.height / 4 + buttonHeight).size(160, 20).tooltip(Tooltip.create(Component.translatable("gui."+EpicFightMod.MODID+".export_custom_armor.tooltip"))).build();
		
		this.optionsList.addSmall(bloodEffectsButton, exportCustomArmors);
		
		buttonHeight += 24;
		
		Button aimingCorrectionButton = Button.builder(Component.translatable("gui."+EpicFightMod.MODID+".aiming_correction." + (aimingCorrection.getValue() ? "on" : "off")), (button) -> {
			aimingCorrection.setValue(!aimingCorrection.getValue());
			button.setMessage(Component.translatable("gui."+EpicFightMod.MODID+".aiming_correction." + (aimingCorrection.getValue() ? "on" : "off")));
		}).pos(this.width / 2 - 165, this.height / 4 + buttonHeight).size(160, 20).tooltip(Tooltip.create(Component.translatable("gui."+EpicFightMod.MODID+".aiming_correction.tooltip"))).build();
		
		Button uiSetupButton = Button.builder(Component.translatable("gui."+EpicFightMod.MODID+".ui_setup"), (button) -> {
			this.minecraft.setScreen(new UISetupScreen(this));
		}).pos(this.width / 2 + 5, this.height / 4 + buttonHeight).size(160, 20).tooltip(Tooltip.create(Component.translatable("gui."+EpicFightMod.MODID+".ui_setup.tooltip"))).build();
		
		this.optionsList.addSmall(aimingCorrectionButton, uiSetupButton);
		
		buttonHeight += 24;
		
		Button showEpicfightAttributesButton = Button.builder(Component.translatable("gui."+EpicFightMod.MODID+".show_attributes." + (showEpicFightAttributes.getValue() ? "on" : "off")), (button) -> {
			showEpicFightAttributes.setValue(!showEpicFightAttributes.getValue());
			button.setMessage(Component.translatable("gui."+EpicFightMod.MODID+".show_attributes." + (showEpicFightAttributes.getValue() ? "on" : "off")));
		}).pos(this.width / 2 - 165, this.height / 4 + buttonHeight).size(160, 20).tooltip(Tooltip.create(Component.translatable("gui."+EpicFightMod.MODID+".show_attributes.tooltip"))).build();
		
		Button maxHitProjectilesButton = Button.builder(Component.translatable("gui."+EpicFightMod.MODID+".max_stuck_projectiles", String.valueOf(maxHitProjectiles.getValue())), (button) -> {
			maxHitProjectiles.setValue((maxHitProjectiles.getValue() + 1) % 30);
			button.setMessage(Component.translatable("gui."+EpicFightMod.MODID+".max_stuck_projectiles", String.valueOf(maxHitProjectiles.getValue())));
		}).pos(this.width / 2 + 5, this.height / 4 + buttonHeight).size(160, 20).tooltip(Tooltip.create(Component.translatable("gui."+EpicFightMod.MODID+".max_stuck_projectiles.tooltip"))).build();
		
		this.optionsList.addSmall(showEpicfightAttributesButton, maxHitProjectilesButton);
		
		buttonHeight += 24;
		
		Button firstPersonModelButton = Button.builder(Component.translatable("gui."+EpicFightMod.MODID+".first_person_model." + (firstPersonModel.getValue() ? "on" : "off")), (button) -> {
			firstPersonModel.setValue(!firstPersonModel.getValue());
			button.setMessage(Component.translatable("gui."+EpicFightMod.MODID+".first_person_model." + (firstPersonModel.getValue() ? "on" : "off")));
		}).pos(this.width / 2 - 165, this.height / 4 + buttonHeight).size(160, 20).tooltip(Tooltip.create(Component.translatable("gui."+EpicFightMod.MODID+".first_person_model.tooltip"))).build();
		
		Button useAnimationShaderButton = Button.builder(Component.translatable("gui."+EpicFightMod.MODID+".use_animation_shader." + (useAnimationShader.getValue() ? "on" : "off")), (button) -> {
			useAnimationShader.setValue(!useAnimationShader.getValue());
			button.setMessage(Component.translatable("gui."+EpicFightMod.MODID+".use_animation_shader." + (useAnimationShader.getValue() ? "on" : "off")));
		}).pos(this.width / 2 + 5, this.height / 4 + buttonHeight).size(160, 20).tooltip(Tooltip.create(Component.translatable("gui."+EpicFightMod.MODID+".use_animation_shader.tooltip"))).build();
		
		if (this.config.shaderModeSwitchingLocked) {
			useAnimationShaderButton.active = false;
			useAnimationShaderButton.setTooltip(Tooltip.create(Component.translatable("gui." + EpicFightMod.MODID + ".use_animation_shader.locked.tooltip")));
		}
		
		this.optionsList.addSmall(firstPersonModelButton, useAnimationShaderButton);
		
		buttonHeight += 30;
		
		this.optionsList.addBig(new ColorWidget(this.width / 2 - 150, this.height / 4 + buttonHeight, 300, 20, Component.translatable("gui."+EpicFightMod.MODID+".aim_helper_color"), aimHelperColor.getValue(), EpicFightMod.CLIENT_CONFIGS.aimHelperColor));
		this.addWidget(this.optionsList);
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		ClientEngine.getInstance().renderEngine.versionNotifier.render(guiGraphics, false);
		this.basicListRender(guiGraphics, this.optionsList, mouseX, mouseY, partialTicks);
	}
}