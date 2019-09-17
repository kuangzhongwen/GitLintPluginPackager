package com.wuba.gitlint

import org.gradle.api.Project

/**
 * 插件业务使用到的工具方法
 *
 * @author kuang on 2019/09/17
 */
class Utils {

    /**
     * 通过 git 命令获取需要检查的文件
     *
     * @param project gradle.Project
     * @return 文件名
     */
    static List<String> getCommitChange(Project project) {
        ArrayList<String> filterList = new ArrayList<>()
        try {
            // 此命令获取本次提交的文件，在 git commit 之后执行
            String command = "git diff --name-only --diff-filter=ACMRTUXB HEAD~1 HEAD~0"
            String changeInfo = command.execute(null, project.getRootDir()).text.trim()
            if (changeInfo == null || changeInfo.empty) {
                return filterList
            }
            String[] lines = changeInfo.split("\\n")
            return lines.toList()
        } catch (Exception e) {
            e.printStackTrace()
            return filterList
        }
    }

    /**
     * 检查特定后缀的文件
     * 比如: .java .kt .xml等
     *
     * @param fileName 文件名
     * @return 匹配 返回 true 否则 返回 false
     */
    static boolean isMatchFile(String[] fileTypes, String fileName) {
        for (String type : fileTypes) {
            if (fileName.endsWith(type)) {
                return true
            }
        }
        return false
    }

    /**
     * 通过 git diff 获取已提交文件的修改,包括文件的添加行的行号、删除行的行号、修改行的行号
     *
     * @param filePath 文件路径
     * @param project Project对象
     * @param startIndex 修改开始的下表数组
     * @param endIndex 修改结束的下表数组
     */
    static void getFileChangeStatus(String filePath, Project project, List<Integer> startIndex, List<Integer> endIndex) {
        try {
            String command = "git diff --unified=0 --ignore-blank-lines --ignore-all-space HEAD~1 HEAD " + filePath
            String changeInfo = command.execute(null, project.getRootDir()).text.trim()
            String[] changeLogs = changeInfo.split("@@")
            String[] indexArray

            for (int i = 1; i < changeLogs.size(); i += 2) {
                indexArray = changeLogs[i].trim().split(" ")
                try {
                    int start, end
                    String[] startArray = null
                    if (indexArray.length > 1) {
                        startArray = indexArray[1].split(",")
                    }

                    if (startArray != null && startArray.length > 1) {
                        start = Integer.parseInt(startArray[0])
                        end = Integer.parseInt(startArray[0]) + Integer.parseInt(startArray[1])
                    } else {
                        start = Integer.parseInt(startArray[0])
                        end = start + 1
                    }
                    startIndex.add(start)
                    endIndex.add(end)
                } catch (NumberFormatException e) {
                    e.printStackTrace()
                    startIndex.add(0)
                    endIndex.add(0)
                }
            }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}