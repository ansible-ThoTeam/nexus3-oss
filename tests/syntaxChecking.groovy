import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.Phases

import java.security.CodeSource
import java.security.cert.Certificate

final File file
final CompilationUnit unit
final URL url

file = new File(args[0])
url = file.toURI().toURL()
unit = new CompilationUnit(
        CompilerConfiguration.DEFAULT,
        new CodeSource(url,(Certificate[])null),
        null
)

unit.addSource(file)
unit.compile(Phases.CONVERSION)
