# jPasi
Originally a Java applet for creating diagrams that exports LaTeX code. The original functionality is still there, and it does run in an IDE. Only the embedding applet is missing, having been deleted long ago when applets became deprecated. I’m currently trying to reimplement this diagram editor in Typescript, in the form of a web application built on Next.js. You can find this ongoing project [here](https://github.com/jplate/pasi).

Here’s a screenshot of *jPasi* running in an IDE:

![Screenshot 2024-05-16 145706](https://github.com/jplate/jPasi/assets/3945422/a416abe3-d8d5-444c-a370-5ec5f184247d)

## Quick Start Guide ##

### Adding and selecting nodes ###
To start creating a diagram, you can click on one or (with <kbd>Shift</kbd>-click) more points on the canvas and then click either on the ‘Add Node’ button to create an equal number of *entity nodes* or on the ‘Add Contour’ button to create one or more groups of eight *contour nodes*, with each group forming a rounded rectangle. The whole app basically revolves around these two kinds of nodes and the operations that can be performed on them. If you click on a given node, it becomes the *primarily selected item*, and its editable parameters are shown in the *editor tab* on the right side of the app. You can select further nodes by shift-clicking on them; the one selected last becomes the new primarily selected item. Another method of selecting nodes involves drawing a ‘lasso’ around them with the mouse. A selected node can be *de*selected by clicking on it while holding <kbd>Ctrl</kbd> (but not <kbd>Shift</kbd>) pressed. If a given set of nodes forms a *group* (as contour nodes do by default), then selecting any one of them while *not* holding <kbd>Ctrl</kbd> pressed also results in the selection of all the others.

### Adding text ###
Text can be added to the diagram by editing labels attached to entity nodes. To create a label, first select an entity node, then select ‘Label’ in the dropdown menu to the top right, and finally click on the ‘Create’ button. In the editor tab, you can then edit the text of the label and, for instance, center it on the node. There is no limit to the number of labels that can be attached to a given node. (A node can be made invisible by setting its line-width to zero in the editor tab.)

### Adding connectors ###
To create a connector (i.e., a line or arrow), you first have to select two or more nodes &ndash; or the same node twice (using <kbd>Shift</kbd>-click). A node can occur more than once in the selection; the positions at which it occurs are indicated by small red numbers. Select the desired connector using the drop-down menu to the right, and click on ‘Create’. This will create one or more connectors, going from the first-selected node to the second, from the second to the third, and so on. To edit a connector, you have to select the associated entity node, which is created together with the connector itself and at first invisible. By default, this entity node is located near the center of the connector it is associated with as well as relatively large, so that it will normally not be difficult to find. (On being selected, it becomes visible and shrinks at the same time.) Deleting this node also removes the associated connector.

### The generated code ###
Once you have finished creating your diagram, you can click on the ‘Generate’ button in the bottom-right corner to generate code that can be pasted into your LaTeX document. This code will appear in a text area that opens up once you click on the ‘Generate’ button. In order for LaTeX to process it, you will need to load Peter Kabal’s [texdraw](https://ctan.org/pkg/texdraw) package in your document’s preamble (using the `\usepackage` command). This code will also allow you to recreate your diagram in *jPasi* at a later point. To do so, first click on the ‘Load’ button near the bottom-right corner. This will open the same text area as before, and now you can paste your code into it. Clicking on the ‘Load’ button that is right beneath that text area will recreate your diagram. (There is also a ‘Replace current diagram’ checkbox that you will need to tick if you do not want your diagram to be added to the one that may already be present on the canvas.)

### Other features ###
- If and when there is no primarily selected item, the editable parameters shown in the editor tab are those of the canvas itself. These include parameters of a *grid* to which nodes tend to ‘snap’ as they are being dragged across the canvas.
- In the *transform tab*, you can perform geometrical transformations (scalings, rotations, and flips) on the selected group of nodes. These operations are centered on the primarily selected item. By default, the sizes of entity nodes are not affected by scaling, but this can be changed by ticking a checkbox.
- In the *group tab*, you can create and manage groups of nodes.
- Two or more connectors running between the same two entity nodes can be combined into a single *compound connector*. To do so, first select the two associated entity nodes (see the above entry on ‘Adding connectors’). The button that is normally labeled ‘Copy Selection’ will then read ‘Combine Connectors’. Clicking on this button will combine the two connectors into one &ndash; useful for creating bidirectional arrows. The two connectors can be decombined by clicking on a button in the editor tab of the resulting compound connector.
- You can edit multiple items at the same time. For example, by editing the text of a label, you’ll be simultaneously editing the text of any other label that happens to be selected at that time.

## Keyboard Shortcuts ##

### General ###
<table>
  <tr>
    <th style="width: 150px;"> Shortcut </th><th style="width: 500px"> Description </th>
  </tr>
 <tr>
    <td>  <kbd>Backspace</kbd> <kbd>Delete</kbd>		</td><td>Deletes the current selection.	</td>
 </tr>
 <tr>
    <td> C	</td><td>Copies selected items or combines two or more selected connectors into a compound, depending on what is selected.	</td>
 </tr>
 <tr>
    <td> D		</td><td>‘Defocuses’ the primarily selected item (i.e., selects all items in its currently ‘highest-level’ active group).	</td>
 </tr>
 <tr>
    <td> E	</td><td>Creates one or more new entity nodes at the selected locations.	</td>
 </tr>
 <tr>
    <td> F	</td><td>‘Focuses’ the primarily selected item (i.e., deselects all other items).	</td>
 </tr>
 <tr>
    <td> K		</td><td> Creates one or more new contours at the selected locations.	</td>
 </tr>
 <tr>
    <td> N		</td><td> Creates a new contour node (requires the primarily selected item to be itself a contour node; the new node will then occupy the preceding position in the contour’s node group).	</td>
 </tr>
 <tr>
    <td> Y		</td><td> Redo (moves one step forward in the recorded editing history).	</td>
 </tr>
 <tr>
    <td> Z		</td><td> Undo (moves one step backward in the recorded editing history).	</td>
 </tr>
 <tr>
    <td>  F5	</td><td> Switches back and forth between two global states: in the default state, all ‘spinners’ (numerical controls with two small ‘up’ and ‘down’ buttons) have their own individual increments, typically greater than 1. In the second state, these increments are all set to 1, so as to allow for finer adjustments.	</td>
 </tr>
</table>

### Input and output ###
<table>
  <tr>
    <th style="width: 150px;"> Shortcut </th><th style="width: 500px"> Description </th>
  </tr>
 <tr>
    <td>   <kbd>Ctrl</kbd>+G </td><td> Generates the LaTeX code for the current diagram and displays it in the output text area.</td>
 </tr>
 <tr>
    <td> <kbd>Ctrl</kbd>+L </td><td> Opens the input text area, or, if it is already open, tries to reconstruct a diagram from the text area’s contents.</td>
 </tr>
 </table>

### Ornaments and Connectors ###
<table>
  <tr>
    <th style="width: 150px;"> Shortcut </th><th style="width: 500px;"> Description </th>
  </tr>
 <tr>
    <td>   <kbd>Space</kbd>	</td><td> Creates one or more connectors or ornaments for the selected entity nodes. In the case of connectors, the first selected node is connected to the second, the second to the third, and so on.</td>
 </tr>
 <tr>
    <td>  <kbd>Shift</kbd>+<kbd>Space</kbd> </td><td> Creates one or more connectors or ornaments for the selected entity nodes. In the case of connectors, the first selected node is connected to each of the other nodes.</td>
 </tr>
</table>

### Transformations ###
Pasi maintains a ‘tranformation type’ variable that can assume one of three values: *rotation*, *scaling*, and *translation* (the default value). These are selected, respectively, by pressing R, S, or T. The actual transformations are then effected (provided that the mouse focus is on the canvas) by pressing the corresponding arrow keys: up and down for rotation and scaling, all four directions for translation. For rotation and scaling, the left- and right-arrow buttons are used to control the rotation or scaling increment (which can also be adjusted using the appropriate controls in the transform tab). Separate keyboard commands are defined for horizontal and vertical flips.

<table>
  <tr>
    <th style="width: 150px;"> Shortcut </th><th style="width: 500px;"> Description </th>
  </tr>
 <tr>
    <td>   <kbd>↑</kbd>	</td><td> Counter-clockwise rotation, positive scaling, or upward movement (depending on the selected transformation type).</td>
 </tr>
 <tr>
    <td>   <kbd>↓</kbd>	</td><td> Clockwise rotation, negative scaling, or downward movement.</td>
 </tr>
 <tr>
    <td>   <kbd>→</kbd>	</td><td> Increase of rotation or scaling increment by factor 10, or rightward movement.</td>
 </tr>
 <tr>
    <td>   <kbd>←</kbd>	</td><td> Decrease of rotation or scaling increment by factor 10, or leftward movement.</td>
 </tr>
 <tr>
    <td>   <kbd>Shift</kbd>+A	</td><td> Toggles scaling of arrow heads.</td>
 </tr>
 <tr>
    <td>   <kbd>Shift</kbd>+E	</td><td> Toggles scaling of entity nodes.</td>
 </tr>
 <tr>
    <td>   <kbd>Shift</kbd>+F	</td><td> Toggles flipping of arrow heads.</td>
 </tr>
 <tr>
    <td>   H	</td><td> Horizontally flips the selected group of nodes.</td>
 </tr>
 <tr>
    <td>   R	</td><td> Selects <i>rotation</i> as current transformation type.</td>
 </tr>
 <tr>
    <td>   S	</td><td> Selects <i>scaling</i> as current transformation type.</td>
 </tr>
 <tr>
    <td>   T	</td><td> Selects <i>translation</i> as current transformation type.</td>
 </tr>
 <tr>
    <td>   V	</td><td> Vertically flips the selected group of nodes.</td>
 </tr>
 <tr>
    <td>   <kbd>Shift</kbd>+W	</td><td> Toggles scaling of line widths and patterns.</td>
 </tr>
</table>

### Managing groups ###
<table>
  <tr>
    <th style="width: 150px;"> Shortcut </th><th style="width: 500px;"> Description </th>
  </tr>
 <tr>
    <td>   G	</td><td> Creates a new group consisting of the selected nodes or (where applicable) their highest-level active groups.</td>
 </tr>
 <tr>
    <td>   J	</td><td> Causes the primarily selected item to rejoin its lowest-level inactive group (i.e., it again becomes an active member of that group).</td>
 </tr>
 <tr>
    <td>   <kbd>Shift</kbd>+J	</td><td> Restores the highest-level active group of the primarily selected item (i.e., reactivates the membership of all its passive members).</td>
 </tr>
 <tr>
    <td>   L	</td><td> Causes the primarily selected item to leave its highest-level active group (i.e., it becomes a passive member of that group).</td>
 </tr>
 <tr>
    <td>   <kbd>Shift</kbd>+L	</td><td> Dissolves the highest-level active group of the primarily selected item (i.e., deactivates the membership of all its currently active members).</td>
 </tr>
</table>
