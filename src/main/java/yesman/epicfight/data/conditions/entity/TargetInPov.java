package yesman.epicfight.data.conditions.entity;

import java.util.List;
import java.util.function.Function;

import io.netty.util.internal.StringUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.datapack.widgets.ResizableEditBox;
import yesman.epicfight.data.conditions.Condition.EntityPatchCondition;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class TargetInPov extends EntityPatchCondition {
	protected double min;
	protected double max;
	
	public TargetInPov() {
	}
	
	public TargetInPov(double min, double max) {
		this.min = min;
		this.max = max;
	}
	
	@Override
	public TargetInPov read(CompoundTag tag) {
		if (!tag.contains("min")) {
			throw new IllegalArgumentException("TargetInPov condition error: min degree not specified!");
		}
		
		if (!tag.contains("max")) {
			throw new IllegalArgumentException("TargetInPov condition error: max degree not specified!");
		}
		
		this.min = tag.getDouble("min");
		this.max = tag.getDouble("max");
		
		return this;
	}
	
	@Override
	public CompoundTag serializePredicate() {
		CompoundTag tag = new CompoundTag();
		tag.putDouble("min", this.min);
		tag.putDouble("max", this.max);
		
		return tag;
	}
	
	@Override
	public boolean predicate(LivingEntityPatch<?> entitypatch) {
		double degree = entitypatch.getAngleTo(entitypatch.getTarget());
		return this.min < degree && degree < this.max;
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public List<ParameterEditor> getAcceptingParameters(Screen screen) {
		ResizableEditBox minEditBox = new ResizableEditBox(screen.getMinecraft().font, 0, 0, 0, 0, Component.literal("min"), null, null);
		ResizableEditBox maxEditBox = new ResizableEditBox(screen.getMinecraft().font, 0, 0, 0, 0, Component.literal("max"), null, null);
		minEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
		maxEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
		Function<Object, Tag> doubleParser = (value) -> DoubleTag.valueOf(Double.valueOf(value.toString()));
		Function<Tag, Object> doubleGetter = (tag) -> ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(tag, Tag::getAsString));
		
		return List.of(ParameterEditor.of(doubleParser, doubleGetter, minEditBox), ParameterEditor.of(doubleParser, doubleGetter, maxEditBox));
	}
	
	public static class TargetInPovHorizontal extends TargetInPov {
		public TargetInPovHorizontal() {
		}
		
		public TargetInPovHorizontal(double min, double max) {
			super(min, max);
		}
		
		@Override
		public boolean predicate(LivingEntityPatch<?> entitypatch) {
			double degree = entitypatch.getAngleToHorizontal(entitypatch.getTarget());
			return this.min < degree && degree < this.max;
		}
	}
}
