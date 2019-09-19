package com.wuba.gitlint

import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.tools.lint.LintCliClient
import com.android.tools.lint.LintStats
import com.android.tools.lint.Main
import com.android.tools.lint.Reporter
import com.android.tools.lint.Warning
import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Position
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.TextFormat
import com.android.utils.SdkUtils
import com.google.common.base.Splitter

/**
 * 重写 Reporter, 以 TXT 格式输出 Lint 扫描结果
 * 通过 LintTxtReporter 来将 Lint 扫描的结果精确到每一行的修改
 *
 * @author kuang on 2019/09/17
 */
class LintTxtReporter extends Reporter {

    private Writer writer

    private List<Integer> startLines
    private List<Integer> endLines

    public int issueNumber = 0

    protected LintTxtReporter(@NonNull LintCliClient client, File output, Writer writer, List<Integer> start, List<Integer> end) {
        super(client, output)
        this.writer = writer
        this.startLines = start
        this.endLines = end
    }

    @Override
    void write(LintStats stats, List<Warning> issues) throws IOException {
        issueNumber = 0
        StringBuilder builder = new StringBuilder(issues.size() * 200)
        builder.append(outputBanner()).append("\n").append("Lint检查日期: " + new Date().toString()).append("\n\n")
        if (issues.isEmpty()) {
            if (isDisplayEmpty()) {
                builder.append("没有扫描结果")
            }
        } else {
            Issue lastIssue = null
            boolean isBetweenNewLines
            int lineNo
            for (Warning warning : issues) {
                isBetweenNewLines = false
                // 输出的行号与文件中对应的行号相差 1, 所以这里进行加 1 操作
                lineNo = warning.line + 1
                /**
                 * 1. 找出扫描结果的行号是否在修改代码之间
                 */
                int size = startLines.size()
                for (int i = 0; i < size; i++) {
                    if (lineNo >= startLines.get(i) && lineNo < endLines.get(i)) {
                        // println("w line " + lineNo + " " + startLines.get(i) + " " + endLines.get(i))
                        isBetweenNewLines = true
                        break
                    }
                }
                /**
                 * 2. 如果 Lint 扫描到的 Issue 不在修改的范围之内, 结束这次循环
                 */
                if (!isBetweenNewLines) {
                    continue
                }
                /**
                 * 3. Lint 扫描的 Issue 在修改的范围内, 将扫描结果写入文件
                 */
                if (warning.issue != lastIssue) {
                    explainIssue(builder, lastIssue)
                    lastIssue = warning.issue
                }

                // 记录 Issue 的数量
                issueNumber++
                String p = warning.path
                if (p != null) {
                    builder.append("(").append(issueNumber).append(")").append("文件名: ")
                    appendPath(builder, p)
                    builder.append('\n').append("问题行号: ")
                    if (warning.line >= 0) {
                        builder.append(Integer.toString(lineNo))
                        builder.append('\n')
                    }
                }

                Severity severity = warning.severity
                if (severity == Severity.FATAL) {
                    severity = Severity.ERROR
                }

                builder.append(severity.getDescription()).append(": ").append(TextFormat.RAW.convertTo(warning.message, TextFormat.TEXT))

                if (warning.issue != null) {
                    builder.append(" [").append(warning.issue.getId()).append(']')
                }
                builder.append('\n')
                if (warning.errorLine != null && !warning.errorLine.isEmpty()) {
                    builder.append("问题代码: ").append(warning.errorLine)
                }
                if (warning.location != null && warning.location.getSecondary() != null) {
                    Location location = warning.location.getSecondary()
                    boolean omitted = false
                    while (location != null) {
                        if (location.getMessage() != null && !location.getMessage().isEmpty()) {
                            builder.append("    ")
                            String path = client.getDisplayPath(warning.project, location.getFile())
                            appendPath(builder, path)

                            Position start = location.getStart()
                            if (start != null) {
                                int line = start.getLine()
                                if (line >= 0) {
                                    builder.append(':').append(Integer.toString(line + 1))
                                }
                            }
                            if (location.getMessage() != null && !location.getMessage().isEmpty()) {
                                builder.append(": ")
                                builder.append(TextFormat.RAW.convertTo(location.message, TextFormat.TEXT))
                            }
                            builder.append('\n')
                        } else {
                            omitted = true
                        }
                        location = location.getSecondary()
                    }

                    if (omitted) {
                        location = warning.location.getSecondary()
                        StringBuilder sbuilder = new StringBuilder(100)
                        sbuilder.append("Also affects: ")
                        int begin = sbuilder.length()
                        while (location != null) {
                            if (location.getMessage() == null || location.getMessage().isEmpty()) {
                                if (sbuilder.length() > begin) {
                                    sbuilder.append(", ")
                                }
                                String path = client.getDisplayPath(warning.project, location.getFile())
                                appendPath(sbuilder, path)
                                Position start = location.getStart()
                                if (start != null) {
                                    int line = start.getLine()
                                    if (line >= 0) {
                                        sbuilder.append(':').append(Integer.toString(line + 1))
                                    }
                                }
                            }
                            location = location.getSecondary()
                        }
                        String wrapped = Main.wrap(sbuilder.toString(), Main.MAX_LINE_WIDTH, "     ")
                        builder.append(wrapped)
                    }
                }

                if (warning.isVariantSpecific()) {
                    List<String> names
                    if (warning.includesMoreThanExcludes()) {
                        builder.append("Applies to variants: ")
                        names = warning.getIncludedVariantNames()
                    } else {
                        builder.append("Does not apply to variants: ")
                        names = warning.getExcludedVariantNames()
                    }
                    builder.append(Joiner.on(", ").join(names)).append('\n')
                }
            }

            if (issueNumber == 0) {
                builder.append("没有扫描结果").append('\n')
            }
            explainIssue(builder, lastIssue)
            builder.append('\n\n')
            builder.append("====================================================================================")
                    .append("\n")
                    .append("+++++++++++++++++++ " + "共发现" + issueNumber + "个Issue,请根据Issue说明的提示信息修改." + "++++++++++++++++++++++++")
                    .append("\n")
                    .append("====================================================================================")
                    .append("\n")
            writer.write(builder.toString())
            writer.write('\n')
            writer.flush()
        }
    }

    private static void appendPath(@NonNull StringBuilder sb, @NonNull String path) {
        sb.append(path)
    }

    /**
     * 对出现的Issue进行说明
     *
     * @param output 输出
     * @param issue Lint Issue
     */
    private static void explainIssue(@NonNull StringBuilder output, @Nullable Issue issue) {
        if (issue == null || issue == IssueRegistry.LINT_ERROR || issue == IssueRegistry.BASELINE) {
            return
        }
        output.append("\n请根据以下提示修改.")
        output.append("\n===================================Issue说明========================================\n")
        String explanation = issue.getExplanation(TextFormat.TEXT)
        if (explanation.trim().isEmpty()) {
            return
        }
        String indent = "   "
        String formatted = SdkUtils.wrap(explanation, Main.MAX_LINE_WIDTH - indent.length(), null)
        output.append('\n')
        output.append(indent)
        output.append("关于 Lint Issue \"").append(issue.getId()).append("\"的说明\n")
        for (String line : Splitter.on('\n').split(formatted)) {
            if (!line.isEmpty()) {
                output.append(indent)
                output.append(line)
            }
            output.append('\n')
        }
        output.append("==================================== end ==========================================\n\n\n")
        List<String> moreInfo = issue.getMoreInfo()
        if (!moreInfo.isEmpty()) {
            for (String url : moreInfo) {
                if (formatted.contains(url)) {
                    continue
                }
                output.append(indent)
                output.append(url)
                output.append('\n')
            }
            output.append('\n')
        }
    }

    private static String outputBanner() {
        StringBuilder builder = new StringBuilder()
        builder.append("====================================================================================")
                .append("\n")
                .append("+++++++++++++++++++++++++++++++++++ Lint扫描结果 ++++++++++++++++++++++++++++++++++++")
                .append("\n")
                .append("====================================================================================")
                .append("\n")
        return builder.toString()
    }
}