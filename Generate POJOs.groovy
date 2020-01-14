import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = "com.ryan.websample.entity;"
typeMapping = [
  (~/(?i)int/)                      : "int",
  (~/(?i)float|double|decimal|real/): "double",
  (~/(?i)datetime|timestamp/)       : "java.sql.Timestamp",
  (~/(?i)date/)                     : "java.sql.Date",
  (~/(?i)time/)                     : "java.sql.Time",
  (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def fields = calcFields(table)
  new File(dir, className + ".java").withPrintWriter { out -> generate(out, className, fields, table) }
}

def generate(out, className, fields, table) {
  out.println "package $packageName"
  out.println ""
  out.println "import javax.persistence.Entity;"
  out.println "import javax.persistence.Table;"
  out.println "import javax.persistence.Id;"
  out.println "import javax.persistence.GeneratedValue;"
  out.println "import java.io.Serializable;"
  out.println ""
  out.println "/**"
  out.println " * @author ryan"
  out.println " */"
  out.println "@Entity"
  out.println "@Table(name = \"" + table.getName() + "\")"
  out.println "public class $className implements Serializable {"
  out.println ""
  out.println genSerialID()
  out.println ""
  out.println "\t@Id"
  out.println "\t@GeneratedValue"
  fields.each() {
    if (it.annos != "") out.println "  ${it.annos}"
    out.println "\tprivate ${it.type} ${it.name};"
  }
  fields.each() {
    out.println ""
    out.println "\tpublic ${it.type} get${it.name.capitalize()}() {"
    out.println "\t\treturn ${it.name};"
    out.println "\t}"
    out.println ""
    out.println "\tpublic void set${it.name.capitalize()}(${it.type} ${it.name}) {"
    out.println "\t\tthis.${it.name} = ${it.name};"
    out.println "\t}"
  }
  out.println "}"
}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
                 name : javaName(col.getName(), false),
                 type : typeStr,
                 annos: ""]]
  }
}

def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
    .collect { Case.LOWER.apply(it).capitalize() }
    .join("")
    .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

def String genSerialID()
{
  return "\tprivate static final long serialVersionUID =  "+Math.abs(new Random().nextLong())+"L;"
}