#!/usr/bin/env python3
"""从 JMH 输出生成反射性能对比图。

用法：
    python docs/benchmark/plot.py <before.txt> <after.txt> <output.png>

依赖：matplotlib
"""
import re
import sys
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

# 中文字体回退（Windows / Linux）
plt.rcParams["font.sans-serif"] = ["Microsoft YaHei", "SimHei", "Noto Sans CJK SC", "DejaVu Sans"]
plt.rcParams["axes.unicode_minus"] = False

PATTERN = re.compile(r"^ReflexInvokeBench\.(\S+)\s+thrpt\s+\d+\s+([\d.]+)")


def parse(path: Path) -> dict:
    """解析 JMH 输出，返回 {基准名: 吞吐量}。"""
    result = {}
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        m = PATTERN.match(line)
        if m:
            result[m.group(1)] = float(m.group(2))
    return result


# 受优化影响的路径
AFFECTED = [
    ("reflexAsm", "Reflex 实例方法\n(有参)"),
    ("reflexInstant", "Reflex Instant 模式\n(有参)"),
    ("unsafeAccessField", "UnsafeAccess\n字段读写"),
]

# 对照组：未受优化影响
CONTROL = [
    ("methodInvoke", "Method.invoke"),
    ("mhGenericCached", "MethodHandle\n(泛化)"),
    ("fieldReflect", "Field.get/set"),
    ("direct", "直接调用"),
]


def draw(group, title, out_path):
    labels, before_vals, after_vals = [], [], []
    for key, label in group:
        if key in before and key in after:
            labels.append(label)
            before_vals.append(before[key])
            after_vals.append(after[key])
    if not labels:
        return

    idx = range(len(labels))
    width = 0.36

    fig, ax = plt.subplots(figsize=(max(7, len(labels) * 1.9), 5.2))
    bars1 = ax.bar([i - width / 2 for i in idx], before_vals, width, label="优化前", color="#b0bec5")
    bars2 = ax.bar([i + width / 2 for i in idx], after_vals, width, label="优化后", color="#42a5f5")

    ax.set_ylabel("吞吐量 (ops/ms，越高越好)")
    ax.set_title(title)
    ax.set_xticks(list(idx))
    ax.set_xticklabels(labels, fontsize=9)
    ax.legend()
    ax.grid(axis="y", linestyle="--", alpha=0.4)

    for bars in (bars1, bars2):
        for bar in bars:
            ax.annotate(f"{bar.get_height():,.0f}",
                        (bar.get_x() + bar.get_width() / 2, bar.get_height()),
                        ha="center", va="bottom", fontsize=8)

    # 标注提升倍数
    for i, (b, a) in enumerate(zip(before_vals, after_vals)):
        if b > 0 and a / b >= 1.5:
            ax.annotate(f"{a / b:.1f}x", (i, max(b, a) * 1.10),
                        ha="center", va="bottom", fontsize=11,
                        fontweight="bold", color="#d32f2f")

    fig.tight_layout()
    fig.savefig(out_path, dpi=140)
    plt.close(fig)


if __name__ == "__main__":
    if len(sys.argv) != 4:
        print(__doc__)
        sys.exit(1)

    before = parse(Path(sys.argv[1]))
    after = parse(Path(sys.argv[2]))
    out = Path(sys.argv[3])
    out.parent.mkdir(parents=True, exist_ok=True)

    draw(AFFECTED, "受优化影响的调用路径（实测提升）",
         out.with_name(out.stem + "-affected" + out.suffix))
    draw(CONTROL, "对照组：未受优化影响，数据持平（验证测量环境一致）",
         out.with_name(out.stem + "-control" + out.suffix))
    print(f"已生成: {out.parent}")
