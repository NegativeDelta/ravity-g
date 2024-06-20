<?php
if (isset($_POST)) {
file_put_contents(time() . ".csv", file_get_contents('php://input'));  
} else {    
    echo "Nie ma tu nic lolz";
}
?>