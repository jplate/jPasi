# jPasi
Originally a Java applet for creating diagrams that exports LaTeX code. The original functionality is still there, and it does run in an IDE. Only the embedding applet is missing, having been deleted long ago when applets became deprecated. I'm currently trying to reimplement this diagram editor in Typescript, in the form of a web application built on Next.js. You can find this ongoing project [here](https://github.com/jplate/pasi).

Here's a screenshot of jPasi running in an IDE:

![Screenshot 2024-05-16 145706](https://github.com/jplate/jPasi/assets/3945422/a416abe3-d8d5-444c-a370-5ec5f184247d)

## Quick Guide ##

### Adding and selecting nodes ###

### Adding connectors ###

### The generated code ###

### Other features ###

- When no node is the primarily selected node, the editable characteristics shown in the editor tab are those of the canvas itself. These include parameters of a grid to which nodes tend to 'snap' as they are being dragged across the canvas.
- In the 'transform tab', you can perform geometrical transformations (scalings, rotations, and flips) on the selected group of nodes. These operations are centered on the primarily selected node. By default, the sizes of entity nodes are not affected by scaling, but this can be changed by ticking a checkbox.
- In the 'group tab', you can create and manage groups of nodes.

## Keyboard shortcuts ##

### General ###
| Shortcut      | Description |
| ----------- | ----------- |
| F5	| Switches back and forth between two global states: In the default state, all 'spinners' (i.e., numerical controls with two little 'up' and 'down' buttons) have their own individual increments, typically greater than 1. In the second state, these increments are all set to 1, so as to allow for finer adjustments.
| Backspace, Delete	| Deletes the current selection.
| C	| Copies selected items or combines connectors into a compound, accordingly as these functions are served by the multi-purpose button in the upper half of the interface.
| D	| 'Defocuses' the primarily selected item (i.e., selects all items in its currently highest active group).
| E	| Creates one or more new entity nodes at the selected locations.
| F	| 'Focuses' the primarily selected item (i.e., deselects all other items).
| K	| Creates one or more new contours at the selected locations.
| N	| Creates a new contour node (requires the primarily selected item to be itself a contour node; the new node will then occupy the preceding position in the contour's node group).
| Y	| Redo (moves one step forward in the recorded editing history).
| Z	| Undo (moves one step backward in the recorded editing history).

### Input and Output ###
| Shortcut      | Description |
| ----------- | ----------- |
| Ctrl+L | Opens the input text area, or, if it is already open, tries to reconstruct a diagram from the text area's contents.
| Ctrl+G | Generates the LaTeX code for the current diagram and displays it in the output text area.
| Ctrl+R | Toggles the {\em Replace current diagram} parameter.

### Ornaments and Connectors ###
| Shortcut      | Description |
| ----------- | ----------- |
| Space	| Creates one or more connectors or ornaments for the selected entity nodes. In the case of connectors, the first selected node is connected to the second, the second to the third, and so on.
| Shift+Space | Creates one or more connectors or ornaments for the selected entity nodes. In the case of connectors, the first selected node is connected to each of the other nodes.

### Transformations ###
Pasi maintains a 'tranformation type' variable that can assume one of three values: *rotation*, *scaling*, and *translation* (the default value). These are selected, respectively, by pressing R, S, or T. The actual transformations are then effected (provided that the mouse focus is on the canvas) by pressing the corresponding arrow keys: up and down for rotation and scaling, all four directions for translation. For rotation and scaling, the left- and right-arrow buttons are used to control the rotation or scaling increment (which can also be adjusted using the appropriate controls in the transform tab). Separate keyboard commands are defined for horizontal and vertical flips.

| Shortcut      | Description |
| ----------- | ----------- |
| ↑	| Counter-clockwise rotation, positive scaling, or upward movement (depending on the selected transformation type).
| ↓	| Clockwise rotation, negative scaling, or downward movement.
| →	| Increase of rotation or scaling increment by factor 10, or rightward movement.
| ←	| Decrease of rotation or scaling increment by factor 10, or leftward movement.
| Shift+A	| Toggles scaling of arrow heads.
| Shift+E	| Toggles scaling of entity nodes.
| Shift+F	| Toggles flipping of arrow heads.
| H	| Horizontally flips the selected group of nodes.
| R	| Selects *rotation* as current transformation type.
| S	| Selects *scaling* as current transformation type.
| T	| Selects *translation* as current transformation type.
| V	| Vertically flips the selected group of nodes.
| Shift+W	| Toggles scaling of line widths and patterns.

### Managing Groups ###
| Shortcut      | Description |
| ----------- | ----------- |
| A	| Toggles adding.
| G	| Creates a new group consisting of the selected nodes or (where applicable) their highest active groups.
| J	| Causes the primarily selected node to 'rejoin' its lowest inactive group (i.e., it again becomes an active member of that group).
| Shift+J	| Restores the highest active group of the primarily selected node (i.e., reactivates the membership of all its passive members).
| L	| Causes the primarily selected node to 'leave' its highest active group (i.e., it becomes a passive member of that group).
| Shift+L	| Dissolves the highest active group of the primarily selected node (i.e., deactivates the membership of all its currently active members).
| M	| Toggles adding of members, as opposed to groups.
