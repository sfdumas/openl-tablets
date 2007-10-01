/**
 * Base class for Editors. If you need to create own layout just override
 * methods of this class.
 *
 * @requires Prototype library
 *
 * @author Andrey Naumenko
 */

var BaseEditor = Class.create();

BaseEditor.prototype = {
// -------------------------------------------------------------- Object properties --
    tableEditor : null,
    node : null,
    td : null,
    initialValue : null,
    isCancelled : false,

    /**
     * Constructor.
     * Generally editor constructor performs the following steps:
     *   1. saves initial cell value into initialValue variable
     *   2. creates an HTML editor control (e.g. HTMLInputElement) and sets its value
     */
    initialize: function(tableEditor, td, param) {
        this.tableEditor = tableEditor;
        this.td = td;
        if (td) {
            // save initial value
            this.initialValue = this.td.innerHTML;
        }
        this.editor_initialize(param);
    },

    editor_initialize: Prototype.emptyFunction,
// ----------------------------------------------------------------- Public methods --

    /** Obtains current value from HTML editor control */
    getValue : function() {
        return this.node ? this.node.value : null;
    },

    /**
     * Destroys HTML editor control, writes value to cell.
     */
    setTDValue : function(/* String */ value) {
        this.td.innerHTML = value;
    },

    /**
     * Destroys HTML editor control, writes value to cell.
     */
    detach : function() {
        var v = this.isCancelled ? this.initialValue : this.getValue();
        this.setTDValue(v);
    },

    /**
     * Releases editor resources.
     */
    destroy : function() {
        // default: do nothing
    },

// ----------------------------------------------------------------- Protected methods --

    /** Notifies table editor that editing is finished */
    doneEdit: function() {
        this.tableEditor.editStop();
    },

    /** Notifies table editor that editing is finished and cancelled */
    cancelEdit: function() {
        this.isCancelled = true;
        this.doneEdit();
    }
}
