windows環境
1. z3の公式からclone
2. READMEのbuild方法に則り、64bit環境のbuidを行う
  - z3/builtディレクトリがある場合、消しておいた方がいい
  - `python scripts/mk_make.py -x --java`を実行
  - x64 Native Command Promptを開き、`nmake`を実行
3. 下のコマンドにより、maven local repositoryにjarファイルを登録
  - `mvn install:install-file -Dfile="C:path-to-z3-dir\z3\build\com.microsoft.z3.jar" -DgroupId="com.microsoft" -DartifactId="z3" -Dversion="4.8.12" -Dpackaging="jar"`
4. Javaのコンパイル・実行時に`-Djava.library.path="C:path-to-z3-dir\z3\build"`を指定
  - Intellij ideaなら"Add VM Options"に指定
5. プログラム中の使用前に、`System.load`で`libz3`と`libz3java`を読み込み
  - windowsならdllファイル
