package io.github.jsnimda.inventoryprofiles.parser

import io.github.jsnimda.common.Log
import io.github.jsnimda.common.Savable
import io.github.jsnimda.common.extensions.*
import io.github.jsnimda.common.gui.widgets.ButtonWidget
import io.github.jsnimda.common.gui.widgets.ConfigButtonInfo
import io.github.jsnimda.common.util.LogicalStringComparator
import io.github.jsnimda.common.vanilla.VanillaUtil
import io.github.jsnimda.common.vanilla.alias.I18n
import io.github.jsnimda.common.vanilla.loggingPath
import io.github.jsnimda.inventoryprofiles.client.TellPlayer
import io.github.jsnimda.inventoryprofiles.event.LockSlotsHandler
import io.github.jsnimda.inventoryprofiles.item.rule.file.RuleFile
import io.github.jsnimda.inventoryprofiles.item.rule.file.RuleFileRegister
import java.util.*
import kotlin.concurrent.schedule

private val strCmpLogical = LogicalStringComparator.file()

object ReloadRuleFileButtonInfo : ConfigButtonInfo() {
  override val buttonText: String
    get() = I18n.translate("inventoryprofiles.gui.config.button.reload_rule_files")

  override fun onClick(widget: ButtonWidget) {
    TellPlayer.listenLog(Log.LogLevel.INFO) {
      RuleLoader.reload()
    }
    widget.active = false
    widget.text = I18n.translate("inventoryprofiles.gui.config.button.reload_rule_files.reloaded")
    Timer().schedule(5000) { // reset after 5 sec
      widget.text = buttonText
      widget.active = true
    }
    val fileNames = RuleFileRegister.loadedFileNames.filter { it != RuleLoader.internalFileDisplayName }
    TellPlayer.chat("Reloaded ${fileNames.size} files: $fileNames")
  }
}

object OpenConfigFolderButtonInfo : ConfigButtonInfo() {
  override val buttonText: String
    get() = I18n.translate("inventoryprofiles.gui.config.button.open_config_folder")

  override fun onClick(widget: ButtonWidget) {
    VanillaUtil.open(configFolder.toFile())
  }
}

private val configFolder = VanillaUtil.configDirectory("inventoryprofiles")
private fun getFiles(regex: String) =
  configFolder.listFiles(regex).sortedWith { a, b -> strCmpLogical.compare(a.name, b.name) }

private val definedLoaders: List<Loader> = listOf(LockSlotsLoader, RuleLoader)

// ============
// loader
// ============

interface Loader {
  fun reload()
}

object CustomDataFileLoader {
  private val loaders = mutableListOf<Loader>()

  fun load() {
    reload()
  }

  fun reload() {
    loaders.forEach { it.reload() }
  }

  init {
    loaders.addAll(definedLoaders)
  }
}

// ============
// lock slots loader
// ============

object LockSlotsLoader : Loader, Savable {
  val file = configFolder / "lockSlots.txt"

  private var cachedValue = listOf<Int>()

  override fun save() {
    try {
      val slotIndices = LockSlotsHandler.lockedInvSlotsStoredValue.sorted()
      if (slotIndices == cachedValue) return
      cachedValue = slotIndices
      slotIndices.joinToString("\n").writeToFile(file)
    } catch (e: Exception) {
      Log.error("Failed to write file ${file.loggingPath}")
    }
  }

  override fun load() {
    try {
      if (!file.exists()) return
      val content = file.readToString()
      val slotIndices = content.lines().mapNotNull { it.trim().toIntOrNull() }
      LockSlotsHandler.lockedInvSlotsStoredValue.apply {
        clear()
        addAll(slotIndices)
      }
      cachedValue = slotIndices
    } catch (e: Exception) {
      Log.error("Failed to read file ${file.loggingPath}")
    }
  }

  override fun reload() {
    load()
  }
}

// ============
// rule loader
// ============
object RuleLoader : Loader {
  const val internalFileDisplayName = "<internal rules.txt>"
  private val internalRulesTxtContent
    get() = VanillaUtil.getResourceAsString("inventoryprofiles:config/rules.txt") ?: ""
      .also { Log.error("Failed to load in-jar file inventoryprofiles:config/rules.txt") }
  private const val regex = "^rules\\.(?:.*\\.)?txt\$"

  override fun reload() {
    Log.clearIndent()
    Log.trace("[-] Rule reloading...")
    val files = getFiles(regex)
    val ruleFiles = mutableListOf(RuleFile(internalFileDisplayName, internalRulesTxtContent))
    for (file in files) {
      try {
        Log.trace("    Trying to read file ${file.name}")
        val content = file.readToString()
        ruleFiles.add(RuleFile(file.name, content))
      } catch (e: Exception) {
        Log.error("Failed to read file ${file.loggingPath}")
      }
    }
    Log.trace("[-] Total ${ruleFiles.size} rule files (including <internal>)")
    RuleFileRegister.reloadRuleFiles(ruleFiles)
    Log.trace("Rule reload end")

    TemporaryRuleParser.onReload()
  }
}