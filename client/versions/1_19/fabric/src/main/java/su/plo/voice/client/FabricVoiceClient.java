package su.plo.voice.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import su.plo.voice.api.client.connection.ServerConnection;
import su.plo.voice.client.connection.FabricClientChannelHandler;
import su.plo.voice.client.gui.VoiceSettingsScreen;
import su.plo.voice.client.render.HudIconRenderer;

import java.io.File;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Environment(EnvType.CLIENT)
public final class FabricVoiceClient extends VoiceClientMod implements ClientModInitializer {

    private final FabricClientChannelHandler handler = new FabricClientChannelHandler(this);

    @Override
    public void onInitializeClient() {
        super.onInitialize();

        // todo: должно ли это быть тут?
        ClientLifecycleEvents.CLIENT_STOPPING.register((minecraft) -> super.onShutdown());

        HudIconRenderer voiceHud = new HudIconRenderer(this, config);
        HudRenderCallback.EVENT.register((__, ___) -> voiceHud.render());

        ClientPlayNetworking.registerGlobalReceiver(CHANNEL, handler);

        var menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "PV settings",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_V,
                        "Plasmo Voice"
                )
        );
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            final LocalPlayer player = minecraft.player;
            if (player == null) return;

            if (menuKey.consumeClick()) {
                if (minecraft.screen instanceof VoiceSettingsScreen) {
                    minecraft.setScreen(null);
                } else {
                    minecraft.setScreen(new VoiceSettingsScreen(minecraft, this, config));
                }
            }
        });
    }

    @Override
    public @NotNull String getVersion() {
        ModContainer modContainer = FabricLoader.getInstance()
                .getModContainer(modId)
                .orElse(null);
        checkNotNull(modContainer, "modContainer cannot be null");
        return modContainer.getMetadata().getVersion().getFriendlyString();
    }

    @Override
    protected File configFolder() {
        return new File("config/" + modId);
    }

    @Override
    protected File modsFolder() {
        return new File("mods");
    }

    @Override
    protected File addonsFolder() {
        return new File(configFolder(), "addons");
    }

    @Override
    public Optional<ServerConnection> getServerConnection() {
        return handler.getConnection();
    }
}
