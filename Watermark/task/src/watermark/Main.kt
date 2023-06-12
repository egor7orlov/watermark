package watermark

import java.awt.Color
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Exception
import java.lang.NumberFormatException
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.system.exitProcess

enum class WatermarkPlacementType {
    SINGLE, GRID
}

class WatermarkPlacement(val placementType: WatermarkPlacementType, val position: Pair<Int, Int>? = null)

fun main() {
    val image = askForImage()
    val watermark = askForWatermark()

    if (image.width < watermark.width || image.height < watermark.height) {
        printProblemMessageAndExitProcess("The watermark's dimensions are larger.")
    }

    val transparencyColor = if (!watermark.colorModel.hasAlpha()) {
        askForTransparencyColor()
    } else {
        null
    }
    val useWatermarkAlpha = if (transparencyColor == null) {
        getUseWatermarkAlpha(watermark)
    } else {
        false
    }
    val transparency = askForWatermarkTransparencyPercentage()
    val watermarkPlacement = askForWatermarkPlacement(image = image, watermark = watermark)
    val outputFileName = askForOutputImageFileName()
    val bufferedOutputImage = getWatermarkedImage(
        image = image,
        watermark = watermark,
        transparency = transparency,
        watermarkPlacement = watermarkPlacement,
        useWatermarkAlpha = useWatermarkAlpha,
        transparencyColor = transparencyColor
    )

    writeImageIntoFile(bufferedOutputImage, outputFileName)
}

fun askForImage(): BufferedImage {
    println("Input the image filename:")

    val imageFile = askForFile()
    val image = ImageIO.read(imageFile)

    when {
        image.colorModel.numColorComponents != 3 -> {
            printProblemMessageAndExitProcess("The number of image color components isn't 3.")
        }

        image.colorModel.pixelSize != 24 && image.colorModel.pixelSize != 32 -> {
            printProblemMessageAndExitProcess("The image isn't 24 or 32-bit.")
        }
    }

    return image
}

fun askForWatermark(): BufferedImage {
    println("Input the watermark image filename:")

    val watermarkFile = askForFile()
    val watermark = ImageIO.read(watermarkFile)

    when {
        watermark.colorModel.numColorComponents != 3 -> {
            printProblemMessageAndExitProcess("The number of watermark color components isn't 3.")
        }

        watermark.colorModel.pixelSize != 24 && watermark.colorModel.pixelSize != 32 -> {
            printProblemMessageAndExitProcess("The watermark isn't 24 or 32-bit.")
        }
    }

    return watermark
}

fun askForTransparencyColor(): Color? {
    println("Do you want to set a transparency color?")

    if (readln().lowercase() != "yes") {
        return null
    }

    println("Input a transparency color ([Red] [Green] [Blue]):")

    try {
        val rgbNums = readln().split(" ").map { it.toInt(10) }

        if (rgbNums.size != 3 || rgbNums.any { it !in 0..255 }) {
            throw Exception("Number is out of range of RGB color")
        }

        return Color(rgbNums[0], rgbNums[1], rgbNums[2])
    } catch (e: Exception) {
        printProblemMessageAndExitProcess("The transparency color input is invalid.")
    }
}

fun getUseWatermarkAlpha(watermark: BufferedImage): Boolean {
    var useWatermarkAlpha = false

    if (watermark.colorModel.transparency == Transparency.TRANSLUCENT) {
        println("Do you want to use the watermark's Alpha channel?")
        useWatermarkAlpha = readln().lowercase() == "yes"
    }

    return useWatermarkAlpha
}

fun askForFile(): File {
    val fileName = readln()
    val file = File(fileName)

    if (!file.exists()) {
        printProblemMessageAndExitProcess("The file $fileName doesn't exist.")
    }

    return file
}

fun askForWatermarkTransparencyPercentage(): Int {
    println("Input the watermark transparency percentage (Integer 0-100):")

    try {
        val percentage = readln().toInt()

        if (percentage !in 0..100) {
            printProblemMessageAndExitProcess("The transparency percentage is out of range.")
        }

        return percentage
    } catch (e: NumberFormatException) {
        printProblemMessageAndExitProcess("The transparency percentage isn't an integer number.")
    }
}

fun askForWatermarkPlacement(image: BufferedImage, watermark: BufferedImage): WatermarkPlacement {
    println("Choose the position method (single, grid):")

    val singlePlacementName = "single"
    val gridPlacementName = "grid"
    val placementMethodInput = readln().lowercase()

    if (!arrayOf(singlePlacementName, gridPlacementName).contains(placementMethodInput)) {
        printProblemMessageAndExitProcess("The position method input is invalid.")
    }

    if (placementMethodInput == gridPlacementName) {
        return WatermarkPlacement(placementType = WatermarkPlacementType.GRID)
    }

    val xUpperConstraint = image.width - watermark.width
    val yUpperConstraint = image.height - watermark.height

    println("Input the watermark position ([x 0-$xUpperConstraint] [y 0-$yUpperConstraint]):")

    try {
        val watermarkPositionCoords = readln().split(" ").map { it.toInt() }

        if (watermarkPositionCoords.size != 2) {
            throw Exception("Invalid watermark position coordinates amount (should be 2: x and y)")
        }

        val x = watermarkPositionCoords[0]
        val y = watermarkPositionCoords[1]

        if (x !in 0..xUpperConstraint || y !in 0..yUpperConstraint) {
            printProblemMessageAndExitProcess("The position input is out of range.")
        }

        return WatermarkPlacement(placementType = WatermarkPlacementType.SINGLE, position = Pair(x, y))
    } catch (e: Exception) {
        printProblemMessageAndExitProcess("The position input is invalid.")
    }
}

fun askForOutputImageFileName(): String {
    println("Input the output image filename (jpg or png extension):")

    val outputFileName = readln()
    val extension = Path(outputFileName).extension

    if (extension != "jpg" && extension != "png") {
        printProblemMessageAndExitProcess("The output file extension isn't \"jpg\" or \"png\".")
    }

    return outputFileName
}

/** I hope you like spaghetti 0__0 */
fun getWatermarkedImage(
    image: BufferedImage,
    watermark: BufferedImage,
    transparency: Int,
    watermarkPlacement: WatermarkPlacement,
    useWatermarkAlpha: Boolean,
    transparencyColor: Color?
): BufferedImage {
    val watermarkedImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
    val watermarkOffsetX = watermarkPlacement.position?.first ?: 0
    val watermarkOffsetY = watermarkPlacement.position?.second ?: 0

    for (x in 0 until watermarkedImage.width) {
        for (y in 0 until watermarkedImage.height) {
            val imagePixel = Color(image.getRGB(x, y))
            val watermarkPixel = if (watermarkPlacement.placementType == WatermarkPlacementType.SINGLE) {
                if (
                    x - watermarkOffsetX in 0 until watermark.width &&
                    y - watermarkOffsetY in 0 until watermark.height
                ) {
                    Color(
                        watermark.getRGB(x - watermarkOffsetX, y - watermarkOffsetY),
                        useWatermarkAlpha
                    )
                } else {
                    imagePixel
                }
            } else {
                Color(
                    watermark.getRGB(
                        if (x < watermark.width) {
                            x
                        } else {
                            x % watermark.width
                        },
                        if (y < watermark.height) {
                            y
                        } else {
                            y % watermark.height
                        }
                    ), useWatermarkAlpha
                )
            }
            val blendedPixel =
                if ((useWatermarkAlpha && watermarkPixel.alpha == 0) || (transparencyColor != null && watermarkPixel == transparencyColor)) {
                    imagePixel
                } else {
                    Color(
                        (transparency * watermarkPixel.red + (100 - transparency) * imagePixel.red) / 100,
                        (transparency * watermarkPixel.green + (100 - transparency) * imagePixel.green) / 100,
                        (transparency * watermarkPixel.blue + (100 - transparency) * imagePixel.blue) / 100
                    )
                }

            watermarkedImage.setRGB(x, y, blendedPixel.rgb)
        }
    }

    return watermarkedImage
}

fun writeImageIntoFile(image: BufferedImage, fileName: String) {
    val file = File(fileName)

    ImageIO.write(image, file.extension, file)
    println("The watermarked image ${file.path} has been created.")
}

fun printProblemMessageAndExitProcess(message: String, exitStatus: Int = 1): Nothing {
    println(message)
    exitProcess(exitStatus)
}
