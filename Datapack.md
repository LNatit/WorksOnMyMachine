### WOMM 世界模板数据定义格式

世界模板的数据包文件统一存放于：

```
data/<modid>/world_templates
```


#### 数据包内容定义格式

JSON 根对象下包含以下字段（带 `#` 标记的为可选条目）：

| 字段                    | 类型 / 标记               |
| ----------------------- | ------------------------- |
| identity                | `#Str`                    |
| alwaysRecreate          | `bool`                    |
| gameType                | `GameType`                |
| difficulty              | `Difficulty`              |
| hardcore                | `bool`                    |
| locked                  | `bool`                    |
| dataConfig              | `#WorldDataConfiguration` |
| preset                  | `*WorldPreset`            |
| seed                    | `#long`                   |
| generateStructures      | `bool`                    |
| generateBonusChest      | `bool`                    |
| gameRules               | `#GameRuleMap`            |

---

#### 字段说明与默认规则

**可选字段（`#`）** 若未显式指定，将应用以下默认规则：

- **`identity`**：预设的唯一识别信息。未指定时优先采用文件名；若该文件名对应的模板已存在，则自动使用 `<modid>:<文件名>`。
- **`dataConfig`**：包含启用的数据包和 FeatureFlag 设定。未指定时默认使用原版内容。详细格式请参考：`net.minecraft.world.level.WorldDataConfiguration`
- **`seed`**：世界生成种子。未指定时随机生成。
- **`gameRules`**：游戏规则列表。未指定时使用原版默认值。详细格式请参考：`net.minecraft.world.level.gamerules.GameRuleMap`

---

**特殊说明：**

- **`preset`** 项请使用字符串指定 WorldPreset 数据包的引用。
  - 参考文档：[世界预设定义格式 - 中文 Minecraft Wiki](https://zh.minecraft.wiki/w/%E4%B8%96%E7%95%8C%E9%A2%84%E8%AE%BE%E5%AE%9A%E4%B9%89%E6%A0%BC%E5%BC%8F#)

---

> 本作品采用 [知识共享署名 4.0 国际许可协议](https://creativecommons.org/licenses/by/4.0/) 进行许可。
