## 本机正常 / Works on my machine

### 简介

WOMM 是一个功能性的 Minecraft 模组，其提供了服务端控制客户端进入本地世界的能力。WOMM 假定用户使用的客户端由服务端提供商统一分发，并且客户端的相关文件未被玩家修改。在这一前提下，WOMM 允许服务端控制客户端进入预设好的（当然也可以是完全随机的）本地世界，提供服务器世界到本地世界的无缝切换体验。典型地，WOMM 适合用于大型模组展会服务器，为玩家提供自由度高、互不干扰、服务器开销低的本地世界体验；同样地，WOMM 也可以适用于科技类整合包，为玩家提供快速的创造设计-生存实践体验。

> 破坏上述假定并不会导致 WOMM 崩溃，但可能会导致客户端进入的世界与预期不一致

### WOMM 世界预设

WOMM 所使用的世界预设主要分为存档模板和数据模板两个组成部分，他们共同提供了 WOMM 强大的世界自定义能力。存档模板和数据模板两者相互独立，仅靠 identity 实现身份识别，因此不需要额外指定关联。

#### 存档模板

存档模板是一个存档文件夹，与一般的存档不同，它允许创建者任意缺省以适配自定义世界的需要。在 WOMM 创建新世界时，存档模板中的文件将会被直接复制到新世界存档文件中，而其中的缺省部分则会经过原版创建世界流程补齐，从而满足在世界中预先加载区块、预先搭建建筑和预先放置物品的需要。

> 是否存档中的任意文件都可以缺省，且缺省后不影响世界的正常加载仍需要更多测试/验证

#### 数据模板

数据模板是一个数据包文件，定义了一个世界的基本属性和生成规则。简单来说，数据模板可以等价于客户端创建世界时的世界选项，其提供了世界的基本属性和生成规则。特别地，如果某个 identity 对应的存档模板本身就是一个完整的客户端存档，则数据模板中的某些字段可能永远都不会被使用。

> 尽管如此，为每个 identity 提供一个数据模板仍然是必须的，因为数据模板中还额外存储了一些 WOMM 正常运行所需的必要信息

### WOMM 数据模板数据定义格式

WOMM 数据模板的数据包文件统一存放于：

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

- **`identity`**：预设的唯一识别名。未指定时优先采用文件名；若该文件名对应的模板已存在，则自动使用 `<modid>:<文件名>`。
- **`dataConfig`**：包含启用的数据包和 FeatureFlag 设定。未指定时默认使用原版内容。详细格式请参考：`net.minecraft.world.level.WorldDataConfiguration`
- **`seed`**：世界生成种子。未指定时随机生成。
- **`gameRules`**：游戏规则列表。未指定时使用原版默认值。详细格式请参考：`net.minecraft.world.level.gamerules.GameRuleMap`

**必填字段补充说明：**

- **`alwaysRecreate`**：当设置为 `true` 时，每次调用都将创建一个新世界；设置为 `false` 时，则会优先进入具有相同 `identity` 的已创建世界。

---

**特殊说明：**

- **`preset`** 项请使用字符串指定 WorldPreset 数据包的引用。
  - 参考文档：[世界预设定义格式 - 中文 Minecraft Wiki](https://zh.minecraft.wiki/w/%E4%B8%96%E7%95%8C%E9%A2%84%E8%AE%BE%E5%AE%9A%E4%B9%89%E6%A0%BC%E5%BC%8F#)

---

> 本作品采用 [知识共享署名 4.0 国际许可协议](https://creativecommons.org/licenses/by/4.0/) 进行许可。
