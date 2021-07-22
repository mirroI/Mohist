package net.minecraftforge.forge.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

abstract class CheckSAS extends DefaultTask {
	@InputFile abstract RegularFileProperty getInheritance()
	@InputFiles abstract ConfigurableFileCollection getSass()
	
    @TaskAction
    protected void exec() {
		Util.init()
		def json = inheritance.get().asFile.json()
		
		sass.each { f -> 
			def lines = []
			f.eachLine { line ->
				if (line[0] == '\t') return //Skip any tabed lines, those are ones we add
				def idx = line.indexOf('#')
				if (idx == 0 || line.isEmpty()) {
					lines.add(line)
					return
				}
				
				def comment = idx == -1 ? null : line.substring(idx)
				if (idx != -1) line = line.substring(0, idx - 1)
				
				def (cls, desc) = (line.trim() + '    ').split(' ', -1)
				cls = cls.replaceAll('\\.', '/')
				desc = desc.replace('(', ' (')
				if (desc.isEmpty() || json[cls] == null || json[cls]['methods'] == null || json[cls]['methods'][desc] == null) {
					println('Invalid: ' + line)
					return
				}
				
				def mtd = json[cls]['methods'][desc]
				lines.add(cls + ' ' + desc.replace(' ', '') + (comment == null ? '' : ' ' + comment))
				def children = json.values().findAll{ it.methods != null && it.methods[desc] != null && it.methods[desc].override == cls}
				.collect { it.name + ' ' + desc.replace(' ', '') } as TreeSet
				children.each { lines.add('\t' + it) }
			}
			f.text = lines.join('\n')
		}
	}
}
