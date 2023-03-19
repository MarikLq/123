package su.plo.mod.client.render

import com.google.common.base.Charsets
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer
import java.util.*

fun registerBase64Texture(texture: String, textureLocation: ResourceLocation) {
    val base64string = texture.substringAfter("base64;")
    val base64bytes = Base64.getDecoder().decode(
        base64string.replace("\n".toRegex(), "").toByteArray(Charsets.UTF_8)
    )

    // register base64 icon in minecraft resources
    RenderSystem.recordRenderCall {
        MemoryStack.stackPush().use { memoryStack ->
            val byteBuffer: ByteBuffer = memoryStack.malloc(base64bytes.size)
            byteBuffer.put(base64bytes)
            byteBuffer.rewind()

            Minecraft.getInstance().textureManager.register(
                textureLocation,
                DynamicTexture(NativeImage.read(byteBuffer))
            )
        }
    }
}
