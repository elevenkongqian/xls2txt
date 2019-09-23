1.将要转换的xls文件拷贝到 xls文件夹下
2.执行start.bat批处理程序(双击就执行了)
3.到txt文件夹下查看转换后的txt文件
4.配置文件在config文件夹下的conf.properties文件，用文本编辑打开即可修改(我已经配置好，一般不用修改)

PS：
生成的txt文件名规则：原先的xls文件名_xls中sheet名.txt
比如：
demo.xlsx中有一个sheet(sheet名是Sheet1)，那么生成后的txt文件名叫:demo_Sheet1.txt

xls文件夹下支持放入多个xls文件，每个xls文件支持多个sheet
目前支持.xls和.xlsx后缀的文件，文件名字，sheet名支持中文。

执行summary.bat命令，统计txt文件夹下所有文件的统计信息（请确保txt文件夹下的文件都是正式的，不要放入测试的txt），生成后的统计文件
在summary文件夹下的summary_max.txt（最大值）,summary_min.txt（最小值）文件。