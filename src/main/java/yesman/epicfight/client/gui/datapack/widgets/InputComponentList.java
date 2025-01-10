package yesman.epicfight.client.gui.datapack.widgets;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.collect.Lists;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class InputComponentList<T> extends ContainerObjectSelectionList<InputComponentList<T>.InputComponentEntry> {
	private final Screen owner;
	private final List<DataBindingComponent<?, ?>> dataBindingComponent = Lists.newArrayList();
	private InputComponentList<T>.InputComponentEntry lastEntry;
	private InputComponentList<T>.InputComponentEntry focusingEntry;
	
	public InputComponentList(Screen owner, int width, int height, int y0, int y1, int itemHeight) {
		super(owner.getMinecraft(), width, height, y0, y1, itemHeight);
		
		this.owner = owner;
		this.setRenderTopAndBottom(false);
	}
	
	@Override
	public int getRowWidth() {
		return this.width;
	}
	
	@Override
	protected int getScrollbarPosition() {
		return this.x1 - 6;
	}
	
	public int nextStart(int spacing) {
		int xPos;
		
		if (this.lastEntry.children.size() == 0) {
			xPos = 0;
		} else {
			ResizableComponent lastWidget = this.lastEntry.children.get(this.lastEntry.children.size() - 1);
			xPos = lastWidget._getX() + lastWidget._getWidth();
		}
		
		return xPos + spacing;
	}
	
	public void addComponentCurrentRow(ResizableComponent inputWidget) {
		this.lastEntry.children.add(inputWidget);
		
		if (inputWidget instanceof DataBindingComponent<?, ?> dataBindingComponent) {
			this.dataBindingComponent.add(dataBindingComponent);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void setDataBindingComponenets(Object[] values) {
		for (int i = 0; i < values.length; i++) {
			DataBindingComponent<Object, Object> dataBinder = (DataBindingComponent<Object, Object>)this.dataBindingComponent.get(i);
			
			try {
				Consumer<Object> consumer = dataBinder._getResponder();
				dataBinder._setResponder(null);
				dataBinder._setValue(values[i]);
				dataBinder._setResponder(consumer);
				
			} catch (Exception e) {
				e.printStackTrace();
				throw new IllegalStateException(String.format("Error while binding %sst component [%s] because of %s", String.valueOf(i), dataBinder._getMessage(), e.getMessage()));
			}
		}
	}
	
	public void newRow() {
		this.lastEntry = new InputComponentEntry();
		this.addEntry(this.lastEntry);
	}
	
	public void clearComponents() {
		this.children().clear();
		
		this.focusingEntry = null;
		this.dataBindingComponent.clear();
	}
	
	public void setComponentsActive(boolean active) {
		for (InputComponentList<T>.InputComponentEntry e : this.children()) {
			for (ResizableComponent componenet : e.children) {
				componenet._setActive(active);
			}
		}
	}
	
	public void resetComponents() {
		this.dataBindingComponent.forEach((dataBinder) -> dataBinder.reset());
	}
	
	@SuppressWarnings("unchecked")
	public <O extends ResizableComponent> O getComponent(int row, int column) {
		return (O)this.children().get(row).children.get(column);
	}
	
	public abstract void importTag(T tag);
	
	@Override
	public void updateSize(int width, int height, int y0, int y1) {
		super.updateSize(width, height, y0, y1);
		
		for (InputComponentList<T>.InputComponentEntry entry : this.children()) {
			for (ResizableComponent widget : entry.children()) {
				widget.resize(this.getRectangle());
			}
		}
	}
	
	@Override
	public void setLeftPos(int xPos) {
		super.setLeftPos(xPos);
		
		for (InputComponentList<T>.InputComponentEntry entry : this.children()) {
			for (ResizableComponent widget : entry.children()) {
				widget.resize(this.getRectangle());
			}
		}
	}

	public void tick() {
		for (InputComponentList<T>.InputComponentEntry entry : this.children()) {
			for (ResizableComponent widget : entry.children()) {
				widget._tick();
			}
		}
	}
	
	@Override
	public boolean mouseClicked(double x, double y, int button) {
		if (!this.isMouseOver(x, y)) {
			return false;
		}
		
		this.updateScrollingState(x, y, button);
		
		if (this.focusingEntry != null) {
			int i = this.children().indexOf(this.focusingEntry);
			int j1 = this.getRowTop(i);
			int k1 = this.getRowBottom(i);
			
			if (k1 >= this.y0 && j1 <= this.y1) {
				boolean pressed = false;
				
				for (GuiEventListener guiEventListener : this.focusingEntry.children()) {
					boolean mouseClicked = guiEventListener.mouseClicked(x, y, button);
					pressed |= mouseClicked;
					
					if (mouseClicked) {
						this.owner.setFocused(guiEventListener);
					}
				}
				
				if (pressed) {
					return false;
				}
			}
		}
		
		for (int i = 0; i < this.children().size(); i++) {
			InputComponentEntry entry = this.children().get(i);
			
			if (entry == this.focusingEntry) {
				continue;
			}
			
			int j1 = this.getRowTop(i);
			int k1 = this.getRowBottom(i);
			
			if (k1 >= this.y0 && j1 <= this.y1) {
				boolean pressed = false;
				
				for (GuiEventListener guiEventListener : entry.children()) {
					boolean mouseClicked = guiEventListener.mouseClicked(x, y, button);
					pressed |= mouseClicked;
					
					if (mouseClicked) {
						this.owner.setFocused(guiEventListener);
					}
				}
				
				if (pressed) {
					this.focusingEntry = entry;
					return false;
				}
			}
		}
		
		return false;
	}
	
	@Override
	public boolean mouseScrolled(double x, double y, double amount) {
		for (int i = 0; i < this.children().size(); i++) {
			InputComponentEntry entry = this.children().get(i);
			int j1 = this.getRowTop(i);
			int k1 = this.getRowBottom(i);
			
			if (k1 >= this.y0 && j1 <= this.y1) {
				if (entry.getChildAt(x, y).filter((component) -> component.mouseScrolled(x, y, amount)).isPresent()) {
					return true;
				}
			}
		}
		
		return super.mouseScrolled(x, y, amount);
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		for (int i = 0; i < this.children().size(); i++) {
			InputComponentEntry entry = this.children().get(i);
			int j1 = this.getRowTop(i);
			int k1 = this.getRowBottom(i);
			
			if (k1 >= this.y0 && j1 <= this.y1) {
				if (entry.getChildAt(mouseX, mouseY).filter((component) -> component.mouseDragged(mouseX, mouseY, button, dx, dy)).isPresent()) {
					return true;
				}
			}
		}
		
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}
	
	@OnlyIn(Dist.CLIENT)
	public class InputComponentEntry extends ContainerObjectSelectionList.Entry<InputComponentList<T>.InputComponentEntry> {
		final List<ResizableComponent> children = Lists.newArrayList();
		
		@Override
		public Optional<GuiEventListener> getChildAt(double x, double y) {
			for (GuiEventListener guieventlistener : this.children) {
				if (guieventlistener.isMouseOver(x, y)) {
					return Optional.of(guieventlistener);
				}
			}
			
			return Optional.empty();
		}
		
		@Override
		public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
			for (ResizableComponent widget : this.children) {
				widget.relocateY(InputComponentList.this.owner.getRectangle(), top + InputComponentList.this.itemHeight / 2 - widget._getHeight() / 2);
				widget._renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
			}
		}
		
		@Override
		public List<? extends ResizableComponent> children() {
			return this.children;
		}
		
		@Override
		public List<? extends ResizableComponent> narratables() {
			return this.children;
		}
	}
}