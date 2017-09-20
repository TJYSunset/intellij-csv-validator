package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.Block;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvFormatHelper {

    public static int getTextLength(ASTNode node, CodeStyleSettings codeStyleSettings) {
        CsvCodeStyleSettings csvCodeStyleSettings = codeStyleSettings.getCustomSettings(CsvCodeStyleSettings.class);
        String text = node.getText();
        int length = 0;
        if (csvCodeStyleSettings.TABULARIZE && !csvCodeStyleSettings.WHITE_SPACES_OUTSIDE_QUOTES && text.startsWith("\"")) {
            text = text.substring(1, text.length() - 1);
            length = text.trim().length() + 2;
        } else {
            length = node.getTextLength();
        }
        return length;
    }

    public static ASTNode getRoot(ASTNode node) {
        ASTNode parent;
        while ((parent = node.getTreeParent()) != null) {
            node = parent;
        }
        return node;
    }

    public static SpacingBuilder createSpaceBuilder(CodeStyleSettings settings) {
        CsvCodeStyleSettings csvCodeStyleSettings = settings.getCustomSettings(CsvCodeStyleSettings.class);
        SpacingBuilder builder = new SpacingBuilder(settings, CsvLanguage.INSTANCE);
        if (csvCodeStyleSettings.TRIM_LEADING_WHITE_SPACES || csvCodeStyleSettings.TABULARIZE) {
            builder
                    .after(CsvTypes.COMMA).spaceIf(settings.SPACE_AFTER_COMMA)
                    .before(CsvTypes.RECORD).spaces(0)
                    .before(CsvTypes.FIELD).spaces(0);
        } else if (settings.SPACE_AFTER_COMMA) {
            builder.after(CsvTypes.COMMA).spaces(1);
        }

        if (csvCodeStyleSettings.TRIM_TRAILING_WHITE_SPACES || csvCodeStyleSettings.TABULARIZE) {
            builder
                    .before(CsvTypes.COMMA).spaceIf(settings.SPACE_BEFORE_COMMA)
                    .after(CsvTypes.RECORD).spaces(0)
                    .after(CsvTypes.FIELD).spaces(0);
        } else if (settings.SPACE_BEFORE_COMMA) {
            builder.before(CsvTypes.COMMA).spaces(1);
        }

        return builder;
    }

    public static Map<Integer, CsvColumnInfo<ASTNode>> createColumnInfoMap(ASTNode root, CodeStyleSettings settings) {
        Map<Integer, CsvColumnInfo<ASTNode>> columnInfoMap = new HashMap<>();
        ASTNode child = root.getFirstChildNode();
        while (child != null) {
            if (child.getElementType() == CsvTypes.RECORD) {
                Integer column = 0;
                ASTNode subChild = child.getFirstChildNode();
                while (subChild != null) {
                    if (subChild.getElementType() == CsvTypes.FIELD) {
                        int length = getTextLength(subChild, settings);
                        if (!columnInfoMap.containsKey(column)) {
                            columnInfoMap.put(column, new CsvColumnInfo(column, length));
                        } else if (columnInfoMap.get(column).getMaxLength() < length) {
                            columnInfoMap.get(column).setMaxLength(length);
                        }
                        columnInfoMap.get(column).addElement(subChild);
                        ++column;
                    }
                    subChild = subChild.getTreeNext();
                }
            }
            child = child.getTreeNext();
        }
        return columnInfoMap;
    }

    public static boolean isFirstFieldOfRecordQuoted(@Nullable CsvBlock block) {
        if (block != null && block.getNode().getElementType() == CsvTypes.RECORD) {
            List<Block> subBlocks = block.buildChildren();
            return isFieldQuoted((CsvBlock) subBlocks.get(0));
        }
        return false;
    }

    public static boolean isFieldQuoted(@Nullable CsvBlock block) {
        if (block != null && block.getNode().getElementType() == CsvTypes.FIELD) {
            List<Block> subBlocks = block.buildChildren();
            if (subBlocks.size() > 0) {
                AbstractBlock abstractBlock = (AbstractBlock) subBlocks.get(0);
                return abstractBlock.getNode().getElementType() == CsvTypes.QUOTE;
            }
        }
        return false;
    }
}