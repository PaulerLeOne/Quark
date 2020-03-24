package vazkii.quark.oddities.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import vazkii.arl.util.RegistryHelper;
import vazkii.quark.base.Quark;
import vazkii.quark.base.module.Config;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.Module;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.network.QuarkNetwork;
import vazkii.quark.base.network.message.HandleBackpackMessage;
import vazkii.quark.oddities.client.screen.BackpackInventoryScreen;
import vazkii.quark.oddities.container.BackpackContainer;
import vazkii.quark.oddities.item.BackpackItem;

@LoadModule(category = ModuleCategory.ODDITIES, hasSubscriptions = true, requiredMod = Quark.ODDITIES_ID)
public class BackpackModule extends Module {

	@Config(description =  "Set this to true to allow the backpacks to be unequipped even with items in them") 
	public static boolean superOpMode = false;

	public static Item backpack;
	
    public static ContainerType<BackpackContainer> container;

	@OnlyIn(Dist.CLIENT)
	private static boolean backpackRequested;

	@Override
	public void construct() {
		backpack = new BackpackItem(this);
		
		container = IForgeContainerType.create(BackpackContainer::fromNetwork);
		RegistryHelper.register(container, "backpack");
	}
	
	@Override
	public void clientSetup() {
		ScreenManager.registerFactory(container, BackpackInventoryScreen::new);
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public void onOpenGUI(GuiOpenEvent event) {
		PlayerEntity player = Minecraft.getInstance().player;
		if(player != null && isInventoryGUI(event.getGui()) && !player.isCreative() && isEntityWearingBackpack(player)) {
			requestBackpack();
			event.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public void clientTick(ClientTickEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if(isInventoryGUI(mc.currentScreen) && !backpackRequested && isEntityWearingBackpack(mc.player)) {
			requestBackpack();
			backpackRequested = true;
		} else if(mc.currentScreen instanceof BackpackInventoryScreen)
			backpackRequested = false;
	}

	private void requestBackpack() {
		QuarkNetwork.sendToServer(new HandleBackpackMessage(true));
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public void removeCurseTooltip(ItemTooltipEvent event) {
		if(!superOpMode && event.getItemStack().getItem() instanceof BackpackItem)
			for(ITextComponent s : event.getToolTip())
				if(s.getUnformattedComponentText().equals(Enchantments.BINDING_CURSE.getDisplayName(1).getUnformattedComponentText())) {
					event.getToolTip().remove(s);
					return;
				}
	}

	@OnlyIn(Dist.CLIENT)
	private static boolean isInventoryGUI(Screen gui) {
		return gui != null && gui.getClass() == InventoryScreen.class;
	}
	
	public static boolean isEntityWearingBackpack(Entity e) {
		if(e instanceof LivingEntity) {
			LivingEntity living = (LivingEntity) e;
			ItemStack chestArmor = living.getItemStackFromSlot(EquipmentSlotType.CHEST);
			return chestArmor.getItem() instanceof BackpackItem;
		}

		return false;
	}

	public static boolean isEntityWearingBackpack(Entity e, ItemStack stack) {
		if(e instanceof LivingEntity) {
			LivingEntity living = (LivingEntity) e;
			ItemStack chestArmor = living.getItemStackFromSlot(EquipmentSlotType.CHEST);
			return chestArmor == stack;
		}

		return false;
	}

}
