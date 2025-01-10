package yesman.epicfight.client.gui.datapack.screen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.model.JsonModelLoader;
import yesman.epicfight.client.gui.datapack.widgets.Grid;
import yesman.epicfight.client.gui.datapack.widgets.ModelPreviewer;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.HorizontalSizing;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent.VerticalSizing;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.client.gui.datapack.widgets.ResizableEditBox;
import yesman.epicfight.client.gui.datapack.widgets.Static;
import yesman.epicfight.client.gui.datapack.widgets.Grid.GridBuilder.RowEditButton;

@OnlyIn(Dist.CLIENT)
public class ImportModelScreen extends Screen {
	private final SelectModelScreen caller;
	private final Grid meshGrid;
	private final Grid armatureGrid;
	private final ModelPreviewer modelPreviewer;
	private List<PackEntry<String, AnimatedMesh>> userMeshes;
	private List<PackEntry<String, Armature>> userArmatures;
	
	public ImportModelScreen(SelectModelScreen caller) {
		super(Component.literal("register_model_screen"));
		
		this.userMeshes = new ArrayList<>(DatapackEditScreen.getCurrentScreen().getUserMeshes().entrySet().stream().map((entry) -> PackEntry.ofValue(entry.getKey().toString(), entry.getValue())).toList());
		this.userArmatures = new ArrayList<>(DatapackEditScreen.getCurrentScreen().getUserArmatures().entrySet().stream().map((entry) -> PackEntry.ofValue(entry.getKey().toString(), entry.getValue())).toList());
		this.caller = caller;
		this.modelPreviewer = new ModelPreviewer(0, 10, 30, 30, HorizontalSizing.LEFT_RIGHT, VerticalSizing.TOP_BOTTOM, null, null);
		this.minecraft = caller.getMinecraft();
		this.font = caller.getMinecraft().font;
		
		ScreenRectangle screenRect = caller.getRectangle();
		int split = screenRect.width() / 2 - 60;
		
		this.meshGrid = Grid.builder(this, caller.getMinecraft())
								.xy1(8, screenRect.top() + 14)
								.xy2(split - 10, screenRect.height() - 21)
								.rowHeight(26)
								.rowEditable(RowEditButton.REMOVE)
								.transparentBackground(true)
								.rowpositionChanged((rowposition, values) -> {
									this.modelPreviewer.setMesh(() -> this.userMeshes.get(rowposition).getValue());
								})
								.addColumn(Grid.editbox("mesh_name")
												.editWidgetCreated((editbox) -> editbox.setFilter(ResourceLocation::isValidResourceLocation))
												.valueChanged((event) -> this.userMeshes.get(event.rowposition).setPackKey(event.postValue))
												.editable(true)
												.width(180))
								.pressRemove((grid, button) -> {
									grid.removeRow((rowposition) -> this.userMeshes.remove(rowposition));
								})
								.build();
		
		this.armatureGrid = Grid.builder(this, caller.getMinecraft())
								.xy1(8, screenRect.top() + 14)
								.xy2(split - 10, screenRect.height() - 21)
								.rowHeight(26)
								.rowEditable(RowEditButton.REMOVE)
								.transparentBackground(true)
								.addColumn(Grid.editbox("armature_name")
												.editWidgetCreated((editbox) -> editbox.setFilter(ResourceLocation::isValidResourceLocation))
												.valueChanged((event) -> this.userArmatures.get(event.rowposition).setPackKey(event.postValue))
												.editable(true)
												.width(180))
								.pressRemove((grid, button) -> {
									grid.removeRow((rowposition) -> this.userArmatures.remove(rowposition));
								})
								.build();
		
		for (PackEntry<String, AnimatedMesh> entry : this.userMeshes) {
			this.meshGrid.addRowWithDefaultValues("mesh_name", entry.getKey());
		}
		
		for (PackEntry<String, Armature> entry : this.userArmatures) {
			this.armatureGrid.addRowWithDefaultValues("armature_name", entry.getKey());
		}
	}
	
	@Override
	protected void init() {
		ScreenRectangle screenRect = this.getRectangle();
		int widthSplit = screenRect.width() / 2 - 20;
		int heightSplit = screenRect.height() / 2;
		
		this.meshGrid.updateSize(widthSplit - 10, heightSplit - 20, screenRect.top() + 30, heightSplit - 10);
		this.meshGrid.setLeftPos(10);
		this.meshGrid.resize(screenRect);
		
		this.armatureGrid.updateSize(widthSplit - 10, heightSplit - 18, heightSplit + 8, screenRect.bottom() - 30);
		this.armatureGrid.setLeftPos(10);
		this.armatureGrid.resize(screenRect);
		
		this.addRenderableWidget(new Static(this, 10, 100, 14, 15, null, null, Component.translatable("datapack_edit.import_model.meshes"), Component.literal("")));
		this.addRenderableWidget(this.meshGrid);
		this.addRenderableWidget(new Static(this, 10, 100, heightSplit - 8, 15, null, null, Component.translatable("datapack_edit.import_model.armatures"), Component.literal("")));
		this.addRenderableWidget(this.armatureGrid);
		
		this.modelPreviewer.setX1(widthSplit + 10);
		this.modelPreviewer.resize(screenRect);
		
		this.addRenderableWidget(this.modelPreviewer);
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_OK, (button) -> {
			Map<ResourceLocation, AnimatedMesh> userMeshes = DatapackEditScreen.getCurrentScreen().getUserMeshes();
			Map<ResourceLocation, Armature> userArmatures = DatapackEditScreen.getCurrentScreen().getUserArmatures();
			
			userMeshes.clear();
			userArmatures.clear();
			
			this.userMeshes.forEach((packEntry) -> userMeshes.put(new ResourceLocation(packEntry.getKey()), packEntry.getValue()));
			this.userArmatures.forEach((packEntry) -> userArmatures.put(new ResourceLocation(packEntry.getKey()), packEntry.getValue()));
			
			Meshes.build(Minecraft.getInstance().getResourceManager());
			Armatures.build(Minecraft.getInstance().getResourceManager());
			
			userMeshes.forEach(Meshes::addMesh);
			userArmatures.forEach(Armatures::addArmature);
			
			this.onClose();
		}).pos(this.width / 2 - 162, this.height - 26).size(160, 21).build());
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
			this.minecraft.setScreen(new MessageScreen<>("", "Do you want to quit without saving changes?", this,
				(button2) -> {
					this.onClose();
				}, (button2) -> {
					this.minecraft.setScreen(this);
				}, 180, 70));
		}).pos(this.width / 2 + 2, this.height - 26).size(160, 21).build());
	}
	
	@Override
	public void tick() {
		this.meshGrid._tick();
		this.armatureGrid._tick();
	}
	
	@Override
	public void onClose() {
		this.caller.refreshModelList();
		this.minecraft.setScreen(this.caller);
		this.modelPreviewer.onDestroy();
	}
	
	@Override
	public void onFilesDrop(List<Path> paths) {
		this.minecraft.setScreen(new MessageScreen<>("", "Enter the mod id", this,
			(modid) -> {
				this.meshGrid.setValueChangeEnabled(false);
				this.armatureGrid.setValueChangeEnabled(false);
				
				for (Path path : paths) {
					InputStream stream = null;
					
					try {
						File file = path.toFile();
						stream = new FileInputStream(file);
						
						String modelPath = modid + ":" + file.getName().replace(".json", "");
						JsonModelLoader jsonLoader = new JsonModelLoader(stream, new ResourceLocation(modelPath));
						AnimatedMesh mesh = jsonLoader.loadAnimatedMesh(AnimatedMesh::new);
						Armature armature = jsonLoader.loadArmature(Armature::new);
						
						this.userMeshes.add(PackEntry.ofValue(modelPath, mesh));
						this.userArmatures.add(PackEntry.ofValue(modelPath, armature));
						this.meshGrid.addRowWithDefaultValues("mesh_name", modelPath);
						this.armatureGrid.addRowWithDefaultValues("armature_name", modelPath);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							stream.close();
						} catch (IOException e) {
						}
					}
				}
				
				this.meshGrid.setValueChangeEnabled(true);
				this.armatureGrid.setValueChangeEnabled(true);
				this.minecraft.setScreen(this);
			}, (button) -> this.minecraft.setScreen(this), new ResizableEditBox(this.minecraft.font, 0, 0, 0, 16, Component.literal("datapack_edit.import_animation.input"), null, null), 120, 80)
		);
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (this.modelPreviewer.mouseDragged(mouseX, mouseY, button, dx, dy)) {
			return true;
		}
		
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderDirtBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
}