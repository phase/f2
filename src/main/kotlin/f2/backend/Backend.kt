package f2.backend

import f2.ir.IrModule
import java.io.File

abstract class Backend(val irModule: IrModule) {

    abstract fun output(file: File?)

}
