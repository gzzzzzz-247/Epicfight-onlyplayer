package yesman.epicfight.data.conditions.entity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.datapack.widgets.ResizableEditBox;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.data.conditions.Condition.EntityCondition;

public class HasCustomTag extends EntityCondition {
	private final Set<String> allowedTags;
	
	public HasCustomTag(ListTag allowedTags) {
		this.allowedTags = allowedTags.stream().map(Tag::getAsString).collect(Collectors.toUnmodifiableSet());
	}
	
	@Override
	public Condition<Entity> read(CompoundTag tag) {
		return null;
	}
	
	@Override
	public CompoundTag serializePredicate() {
		return null;
	}
	
	@Override
	public boolean predicate(Entity target) {
		for (String tag : this.allowedTags) {
			if (target.getTags().contains(tag)) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public List<ParameterEditor> getAcceptingParameters(Screen screen) {
		ResizableEditBox editbox = new ResizableEditBox(screen.getMinecraft().font, 0, 0, 0, 0, Component.literal("tag"), null, null);
		return List.of(ParameterEditor.of((value) -> StringTag.valueOf(value.toString()), (tag) -> ParseUtil.nullOrToString(tag, Tag::getAsString), (AbstractWidget)editbox));
	}
}