export default function SizeConvertion () {
    return (data) => {
        var gb = data / 1024 / 1024 / 1024;

        if (data) {
            if (gb < 1) {
                return parseInt(data / 1024 / 1024) + " MB";
            }
            return parseInt(data / 1024 / 1024 / 1024) + " GB";
        }
    }
}