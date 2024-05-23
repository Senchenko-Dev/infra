if [[ -f "build.gradle" ]]; then
version_file="build.gradle"
line_app="versions.app_version="
else
version_file="build.gradle.kts"
line_app="version="
fi
while read line
do
if [[ "$line" == *"$line_app"* ]]; then
get_current_version=`echo $line | sed -e 's/'$line_app'//g' | sed "s/'//g" | sed 's/"//g'`
printf "$get_current_version"
break
fi
done < "$version_file"
