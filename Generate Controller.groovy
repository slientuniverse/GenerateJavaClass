import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = "com.ryan.websample.controller;"
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
  def className = javaName(table.getName(), true) + "Controller"
  def repositoryName = javaName(table.getName(), true) + "Service"
  def name = repositoryName[0].toLowerCase() + repositoryName[1..-1] + ";"
  def fields = calcFields(table)
  new File(dir, className + ".java").withPrintWriter { out -> generate(out, className, fields, repositoryName, name) }
}

def generate(out, className, fields, repositoryName, name) {
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
  out.println "@Controller"
  out.println "@RequestMapping(\"/\")"
  out.println "public class $className {"
  out.println ""
  out.println "\t@Autowired"
  out.println "\tprivate $repositoryName $name"
  out.println ""
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