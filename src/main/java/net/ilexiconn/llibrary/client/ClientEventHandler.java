package net.ilexiconn.llibrary.client;

import net.ilexiconn.llibrary.LLibrary;
import net.ilexiconn.llibrary.client.event.PlayerModelEvent;
import net.ilexiconn.llibrary.client.gui.ModUpdateGUI;
import net.ilexiconn.llibrary.client.gui.SnackbarGUI;
import net.ilexiconn.llibrary.client.model.VoxelModel;
import net.ilexiconn.llibrary.client.util.ClientUtils;
import net.ilexiconn.llibrary.server.snackbar.Snackbar;
import net.ilexiconn.llibrary.server.snackbar.SnackbarHandler;
import net.ilexiconn.llibrary.server.update.UpdateHandler;
import net.ilexiconn.llibrary.server.util.ModUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Rectangle;

@SideOnly(Side.CLIENT)
public enum ClientEventHandler {
    INSTANCE;

    private SnackbarGUI snackbarGUI;
    private boolean checkedForUpdates;
    private ModelBase voxelModel = new VoxelModel();

    public void setOpenSnackbar(SnackbarGUI snackbarGUI) {
        this.snackbarGUI = snackbarGUI;
    }

    @SubscribeEvent
    public void onInitGuiPost(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiMainMenu) {
            int offsetX = 0;
            int offsetY = 0;
            int buttonX = event.getGui().width / 2 - 124 + offsetX;
            int buttonY = event.getGui().height / 4 + 48 + 24 * 2 + offsetY;
            while (true) {
                if (buttonX < 0) {
                    if (offsetY <= -48) {
                        buttonX = 0;
                        buttonY = 0;
                        break;
                    } else {
                        offsetX = 0;
                        offsetY -= 24;
                        buttonX = event.getGui().width / 2 - 124 + offsetX;
                        buttonY = event.getGui().height / 4 + 48 + 24 * 2 + offsetY;
                    }
                }

                Rectangle rectangle = new Rectangle(buttonX, buttonY, 20, 20);
                boolean intersects = false;
                for (int i = 0; i < event.getButtonList().size(); i++) {
                    GuiButton button = event.getButtonList().get(i);
                    if (!intersects) {
                        intersects = rectangle.intersects(new Rectangle(button.xPosition, button.yPosition, button.width, button.height));
                    }
                }

                if (!intersects) {
                    break;
                }

                buttonX -= 24;
            }

            event.getButtonList().add(new GuiButton(ClientProxy.UPDATE_BUTTON_ID, buttonX, buttonY, 20, 20, "U"));

            if (!this.checkedForUpdates && !UpdateHandler.INSTANCE.getOutdatedModList().isEmpty()) {
                this.checkedForUpdates = true;
                SnackbarHandler.INSTANCE.showSnackbar(Snackbar.create(I18n.translateToLocal("snackbar.llibrary.updates_found")));
            }
        }
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (!Loader.isModLoaded("Waila")) {
            ItemStack stack = event.getItemStack();
            if (stack != null) {
                String name = ModUtils.getModNameForStack(stack);
                if (name != null) {
                    event.getToolTip().add(TextFormatting.BLUE.toString() + TextFormatting.ITALIC.toString() + name);
                }
            }
        }
    }

    @SubscribeEvent
    public void onButtonPressPre(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.getGui() instanceof GuiMainMenu && event.getButton().id == ClientProxy.UPDATE_BUTTON_ID) {
            ClientProxy.MINECRAFT.displayGuiScreen(new ModUpdateGUI((GuiMainMenu) event.getGui()));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onClientUpdate(TickEvent.ClientTickEvent event) {
        if (this.snackbarGUI == null && !ClientProxy.SNACKBAR_LIST.isEmpty()) {
            this.setOpenSnackbar(ClientProxy.SNACKBAR_LIST.get(0));
            ClientProxy.SNACKBAR_LIST.remove(this.snackbarGUI);
        }
        if (this.snackbarGUI != null) {
            this.snackbarGUI.updateSnackbar();
        }
    }

    @SubscribeEvent
    public void onRenderUpdate(TickEvent.RenderTickEvent event) {
        ClientUtils.updateLast();
    }

    @SubscribeEvent
    public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        if (ClientProxy.MINECRAFT.currentScreen == null && this.snackbarGUI != null && event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR) {
            this.snackbarGUI.drawSnackbar();
        }
    }

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (this.snackbarGUI != null) {
            this.snackbarGUI.drawSnackbar();
        }
    }

    @SubscribeEvent
    public void onRenderModel(PlayerModelEvent.Render event) {
        if (ClientProxy.PATRONS != null && (ClientProxy.MINECRAFT.gameSettings.thirdPersonView != 0 || event.getEntityPlayer() != ClientProxy.MINECRAFT.thePlayer)) {
            for (String name : ClientProxy.PATRONS) {
                if (event.getEntityPlayer().getGameProfile().getId().toString().equals(name)) {
                    GlStateManager.pushMatrix();
                    GL11.glDepthMask(false);
                    GlStateManager.disableLighting();
                    GlStateManager.translate(0.0F, -1.37F, 0.0F);
                    this.renderVoxel(event, 1.1F, 0.23F);
                    GL11.glDepthMask(true);
                    GlStateManager.enableLighting();
                    GlStateManager.translate(0.0F, 0.128F, 0.0F);
                    this.renderVoxel(event, 1.0F, 1.0F);
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    private void renderVoxel(PlayerModelEvent.Render event, float scale, float color) {
        float bob = MathHelper.sin(((float) event.getEntityPlayer().ticksExisted + LLibrary.PROXY.getPartialTicks()) / 15.0F) * 0.1F;
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.rotate(-ClientUtils.interpolate(event.getEntityPlayer().prevRenderYawOffset, event.getEntityPlayer().renderYawOffset, LLibrary.PROXY.getPartialTicks()), 0, 1.0F, 0);
        GlStateManager.color(color, color, color, 1.0F);
        GlStateManager.translate(0.0F, -1.0F + bob, 0.0F);
        GlStateManager.rotate(ClientUtils.interpolate((event.getEntityPlayer().ticksExisted - 1) % 360, event.getEntityPlayer().ticksExisted % 360, LLibrary.PROXY.getPartialTicks()), 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(0.75F, 0.0F, 0.0F);
        GlStateManager.rotate(ClientUtils.interpolate((event.getEntityPlayer().ticksExisted - 1) % 360, event.getEntityPlayer().ticksExisted % 360, LLibrary.PROXY.getPartialTicks()), 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(scale, scale, scale);
        this.voxelModel.render(event.getEntityPlayer(), event.getLimbSwing(), event.getLimbSwingAmount(), event.getRotation(), event.getRotationYaw(), event.getRotationPitch(), event.getScale());
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
}
